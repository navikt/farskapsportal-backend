package no.nav.farskapsportal.backend.asynkron.consumer.joark;

import java.sql.Date;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import no.nav.farskapsportal.consumer.joark.api.AvsenderMottaker;
import no.nav.farskapsportal.consumer.joark.api.AvsenderMottakerIdType;
import no.nav.farskapsportal.consumer.joark.api.Bruker;
import no.nav.farskapsportal.consumer.joark.api.BrukerIdType;
import no.nav.farskapsportal.consumer.joark.api.Dokument;
import no.nav.farskapsportal.consumer.joark.api.DokumentVariant;
import no.nav.farskapsportal.consumer.joark.api.JournalpostType;
import no.nav.farskapsportal.consumer.joark.api.OpprettJournalpostRequest;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.service.PersonopplysningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FarskapsportalJoarkMapper {

  private static final String BEHANDLINGSTEMA_BIDRAG_INKLUSIV_FARSKAP = "ab0322";
  //FIXME: Opprette egen brevkode for farskapserklæring
  private static final String BREVKODE_FARSKAPSERKLAERING = "fe1234";
  private static final String DOKUMENTKATEGORIKODE_ELEKTRONISK_SKJEMA = "ES";
  private static final String DOKUMENTTYPE_PDFA = "PDFA";
  private static final String DOKUMENTVARIANT_FORMAT_ARKIV = "ARKIV";
  private static final String JOURNALFOERENDE_ENHET_VED_AUTOMATISK_JOURNALFOERING = "9999";
  private static final String KANAL_FOR_INNSENDING_NAV_NO = "NAV_NO";
  private static final String TITTEL_INNSENDING = "Elektronisk innsendt farskapserklæring";
  private static final String TITTEL_FARSKAPSERKLAERING = "Farskapserklæring";
  private static final String TEMA_FAR = "FAR";

  @Autowired
  private PersonopplysningService personopplysningService;

  public OpprettJournalpostRequest tilJoark(Farskapserklaering farskapserklaering) {

    var navnDtoMor = personopplysningService.henteNavn(farskapserklaering.getMor().getFoedselsnummer());

    var request = OpprettJournalpostRequest.builder()
        .avsenderMottaker(AvsenderMottaker.builder()
            .navn(navnDtoMor.sammensattNavn())
            .id(farskapserklaering.getMor().getFoedselsnummer())
            .idType(AvsenderMottakerIdType.FNR)
            .build())
        .bruker(Bruker.builder()
            .id(farskapserklaering.getFar().getFoedselsnummer())
            .idType(BrukerIdType.FNR)
            .build())
        .behandlingstema(BEHANDLINGSTEMA_BIDRAG_INKLUSIV_FARSKAP)
        .datoMottatt(
            Date.from(
                farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt().atZone(ZoneId.systemDefault()).toInstant()))
        .eksternReferanseId(farskapserklaering.getMeldingsidSkatt())
        .kanal(KANAL_FOR_INNSENDING_NAV_NO)
        .journalfoerendeEnhet(JOURNALFOERENDE_ENHET_VED_AUTOMATISK_JOURNALFOERING)
        .journalpostType(JournalpostType.INNGAAENDE)
        .tittel(TITTEL_INNSENDING)
        .tema(TEMA_FAR)
        .dokumenter(oppretteListeMedHoveddokument(farskapserklaering)).build();
    return request;
  }

  private List<Dokument> oppretteListeMedHoveddokument(Farskapserklaering farskapserklaering) {

    return Collections.singletonList(Dokument.builder()
        .brevkode(BREVKODE_FARSKAPSERKLAERING)
        .dokumentKategori(DOKUMENTKATEGORIKODE_ELEKTRONISK_SKJEMA)
        .tittel(TITTEL_FARSKAPSERKLAERING)
        .dokumentvarianter(oppretteDokumentVariant(farskapserklaering))
        .build());
  }

  private List<DokumentVariant> oppretteDokumentVariant(Farskapserklaering farskapserklaering) {
    return Collections.singletonList(DokumentVariant.builder()
        .filnavn("farskapserklaering_" + farskapserklaering.getMeldingsidSkatt() + ".pdf")
        .fysiskDokument(farskapserklaering.getDokument().getDokumentinnhold().getInnhold())
        .filtype(DOKUMENTTYPE_PDFA)
        .variantformat(DOKUMENTVARIANT_FORMAT_ARKIV)
        .build());
  }
}
