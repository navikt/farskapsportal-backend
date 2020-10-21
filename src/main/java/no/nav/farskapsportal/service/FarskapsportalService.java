package no.nav.farskapsportal.service;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.SignaturDto;
import no.nav.farskapsportal.exception.FarskapserklaeringIkkeFunnetException;
import no.nav.farskapsportal.exception.HentingAvDokumentFeiletException;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class FarskapsportalService {

  public static final String FEIL_NAVN =
      "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret";

  private final PdfGeneratorConsumer pdfGeneratorConsumer;
  private final DifiESignaturConsumer difiESignaturConsumer;
  private final PersistenceService persistenceService;
  private final PersonopplysningService personopplysningService;

  public BrukerinformasjonResponse henteBrukerinformasjon(String foedselsnummer) {

    // hente rolle
    var brukersForelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

    if (Forelderrolle.MEDMOR.equals(brukersForelderrolle)
        || Forelderrolle.UKJENT.equals(brukersForelderrolle)) {
      return BrukerinformasjonResponse.builder()
          .forelderrolle(brukersForelderrolle)
          .kanOppretteFarskapserklaering(false)
          .gyldigForelderrolle(false)
          .build();
    }

    // Henter påbegynte farskapserklæringer som venter på mors signatur
    Set<FarskapserklaeringDto> farskapserklaeringerSomVenterPaaMor =
        persistenceService.henteFarskapserklaeringerSomManglerMorsSignatur(foedselsnummer, brukersForelderrolle);

    // Henter påbegynte farskapserklæringer som venter på fars signatur
    Set<FarskapserklaeringDto> farskapserklaeringerSomVenterPaaFar =
        persistenceService.henteFarskapserklaeringer(foedselsnummer);

    Set<String> nyligFoedteBarnSomManglerFar = new HashSet<>();

    // har mor noen nyfødte barn uten registrert far?
    if (!Forelderrolle.FAR.equals(brukersForelderrolle)) {
      nyligFoedteBarnSomManglerFar =
          personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(foedselsnummer);
    }

    return BrukerinformasjonResponse.builder()
        .morsVentendeFarskapserklaeringer(farskapserklaeringerSomVenterPaaMor)
        .farsVentendeFarskapserklaeringer(farskapserklaeringerSomVenterPaaFar)
        .fnrNyligFoedteBarnUtenRegistrertFar(nyligFoedteBarnSomManglerFar)
        .build();
  }

  public OppretteFarskapserklaeringResponse oppretteFarskapserklaering(
      String fnrMor, OppretteFarskaperklaeringRequest request) {

    // Bare mor kan oppretteFarskapserklæring
    personopplysningService.kanOpptreSomMor(fnrMor);
    // Kontrollere opplysninger om far i request
    personopplysningService.riktigNavnOgRolle(request.getOpplysningerOmFar(), Forelderrolle.FAR);

    var barn = BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
    if (request.getBarn().getFoedselsnummer() != null
        && !request.getBarn().getFoedselsnummer().isBlank()) {
      barn.setFoedselsnummer(request.getBarn().getFoedselsnummer());
    }

    var navnMor = personopplysningService.henteNavn(fnrMor);

    var mor =
        ForelderDto.builder()
            .foedselsnummer(fnrMor)
            .fornavn(navnMor.getFornavn())
            .mellomnavn(navnMor.getMellomnavn())
            .etternavn(navnMor.getEtternavn())
            .build();

    var far =
        ForelderDto.builder()
            .forelderRolle(Forelderrolle.FAR)
            .foedselsnummer(request.getOpplysningerOmFar().getFoedselsnummer())
            .fornavn(request.getOpplysningerOmFar().getNavn())
            .build();

    var farskapserklaeringDto =
        FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).build();
    var dokumentDto = pdfGeneratorConsumer.genererePdf(farskapserklaeringDto);

    // Opprette signeringsjobb, oppdaterer dokument med status-url og redirect-url-ers
    difiESignaturConsumer.oppretteSigneringsjobb(dokumentDto, mor, far);
    log.info("Lagre farskapserklæring");
    persistenceService.lagreFarskapserklaering(farskapserklaeringDto);

    return OppretteFarskapserklaeringResponse.builder()
        .redirectUrlForSigneringMor(
            dokumentDto.getDokumentRedirectMor().getRedirectUrl().toString())
        .build();
  }

  /**
   * Henter signert dokument. Lagrer pades-url for fremtidige dokument-nedlastinger
   *
   * @param fnrPaaloggetPerson fødselsnummer til pålogget person
   * @param statusQueryToken tilgangstoken fra e-signeringsløsningen
   * @return kopi av signert dokument
   */
  public byte[] henteSignertDokumentEtterRedirect(
      String fnrPaaloggetPerson, String statusQueryToken) {

    var brukersForelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);

    var farskapserklaeringer =
        persistenceService.henteFarskapserklaeringerSomManglerMorsSignatur(
            fnrPaaloggetPerson, brukersForelderrolle);

    if (farskapserklaeringer.size() < 1) {
      throw new FarskapserklaeringIkkeFunnetException(
          "Fant ingen påbegynt farskapserklæring for pålogget bruker");
    }

    var dokumentStatusDto = henteDokumentstatus(statusQueryToken, farskapserklaeringer);

    // filtrerer ut farskapserklæringen statuslenka tilhører
    var aktuellFarskapserklaeringDto =
        farskapserklaeringer.stream()
            .filter(Objects::nonNull)
            .filter(
                fe ->
                    fe.getDokument()
                        .getDokumentStatusUrl()
                        .equals(dokumentStatusDto.getStatuslenke()))
            .collect(Collectors.toSet())
            .stream()
            .findAny()
            .orElseThrow(
                () -> new FarskapserklaeringIkkeFunnetException("Fant ikke farskapserklæring"));

    // oppdatere padeslenke i aktuell farskapserklæring
    aktuellFarskapserklaeringDto.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke());

    // Oppdatere foreldrenes signeringsstatus
    for (SignaturDto signatur : dokumentStatusDto.getSignaturer()) {
      if (aktuellFarskapserklaeringDto
          .getMor()
          .getFoedselsnummer()
          .equals(signatur.getSignatureier())) {
        aktuellFarskapserklaeringDto
            .getDokument()
            .setSignertAvMor(signatur.getTidspunktForSignering());
      } else if (aktuellFarskapserklaeringDto
          .getFar()
          .getFoedselsnummer()
          .equals(signatur.getSignatureier())) {
        aktuellFarskapserklaeringDto
            .getDokument()
            .setSignertAvFar(signatur.getTidspunktForSignering());
      } else {
        throw new HentingAvDokumentFeiletException(
            "Dokumentets signatureiere er forskjellige fra partene som er registrert i farskapserklæringen!");
      }
    }

    // lagre farskapserklæring med padesUrl for henting av dokumentkopier og oppdatert
    // signeringsstatus for foreldrene
    persistenceService.lagreFarskapserklaering(aktuellFarskapserklaeringDto);

    // returnerer kopi av signert dokument
    return difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
  }

  private DokumentStatusDto henteDokumentstatus(
      String statusQueryToken, Set<FarskapserklaeringDto> farskapserklaeringer) {

    Set<URI> dokumentStatuslenker =
        farskapserklaeringer.stream()
            .map(FarskapserklaeringDto::getDokument)
            .map(DokumentDto::getDokumentStatusUrl)
            .collect(Collectors.toSet());

    // Mangler sikker identifisering av hvilken statuslenke tokenet er tilknyuttet. Forelder kan
    // potensielt ha flere farskapserklæringer som er startet men hvor signeringsprosessen ikke
    // er fullført. Returnerer statuslenke som hører til statusQueryToken.
    return difiESignaturConsumer.henteDokumentstatusEtterRedirect(
        statusQueryToken, dokumentStatuslenker);
  }
}
