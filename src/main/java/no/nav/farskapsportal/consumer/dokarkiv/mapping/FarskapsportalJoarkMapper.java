package no.nav.farskapsportal.consumer.dokarkiv.mapping;

import java.sql.Date;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import no.nav.farskapsportal.consumer.bidrag_sak.BidragSakConsumer;
import no.nav.farskapsportal.consumer.dokarkiv.api.AvsenderMottaker;
import no.nav.farskapsportal.consumer.dokarkiv.api.AvsenderMottakerIdType;
import no.nav.farskapsportal.consumer.dokarkiv.api.Bruker;
import no.nav.farskapsportal.consumer.dokarkiv.api.BrukerIdType;
import no.nav.farskapsportal.consumer.dokarkiv.api.Dokument;
import no.nav.farskapsportal.consumer.dokarkiv.api.DokumentVariant;
import no.nav.farskapsportal.consumer.dokarkiv.api.Fagsaksystem;
import no.nav.farskapsportal.consumer.dokarkiv.api.JournalpostType;
import no.nav.farskapsportal.consumer.dokarkiv.api.OpprettJournalpostRequest;
import no.nav.farskapsportal.consumer.dokarkiv.api.Sak;
import no.nav.farskapsportal.consumer.dokarkiv.api.Sakstype;
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
  private static final String TITTEL_INNSENDING = "Elektronisk innsendt farskapserklæring";
  private static final String TITTEL_FARSKAPSERKLAERING = "Farskapserklæring";
  private static final String TEMA_FAR = "FAR";

  @Autowired
  private BrevkodeToDokumentkategoriMapper brevkodeToDokumentkategoriMapper;

  @Autowired
  private BidragSakConsumer bidragSakConsumer;

  @Autowired
  private PersonopplysningService personopplysningService;

  public OpprettJournalpostRequest tilJoark(Farskapserklaering farskapserklaering) {

    var saksnummer = bidragSakConsumer.oppretteNySak();
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
        .journalfoerendeEnhet(JOURNALFOERENDE_ENHET_VED_AUTOMATISK_JOURNALFOERING)
        .journalpostType(JournalpostType.INNGAAENDE)
        .tittel(TITTEL_INNSENDING)
        .tema(TEMA_FAR)
        .sak(mappeTilJoarkSak(saksnummer))
        .dokumenter(oppretteListeMedHoveddokument(farskapserklaering)).build();
    return request;
  }

  private Sak mappeTilJoarkSak(String bisysSaksnummer) {
    return Sak.builder().sakstype(Sakstype.FAGSAK).fagsaksystem(Fagsaksystem.BISYS).fagsakId(bisysSaksnummer).build();
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
