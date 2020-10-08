package no.nav.farskapsportal.service;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.api.ForelderRolle;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.FarskapserklaeringIkkeFunnetException;
import no.nav.farskapsportal.exception.FeilKjoennPaaOppgittFarException;
import no.nav.farskapsportal.exception.OppgittNavnStemmerIkkeMedRegistrertNavnException;
import no.nav.farskapsportal.persistence.PersistenceService;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class FarskapsportalService {

  public static final String FEIL_NAVN =
      "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret";
  private final PdlApiConsumer pdlApiConsumer;
  private final PdfGeneratorConsumer pdfGeneratorConsumer;
  private final DifiESignaturConsumer difiESignaturConsumer;
  private final PersistenceService persistenceService;

  public HttpResponse<Kjoenn> henteKjoenn(String foedselsnummer) {
    return pdlApiConsumer.henteKjoenn(foedselsnummer);
  }

  public HttpResponse<Void> riktigNavnOgKjoennOppgittForFar(
      KontrollerePersonopplysningerRequest request) {
    Validate.isTrue(request.getFoedselsnummer() != null);

    log.info("Sjekker fars kjønn");
    kontrollereKjoennFar(request.getFoedselsnummer());

    NavnDto navnDto = hentNavnTilPerson(request);

    // Validere input
    Validate.isTrue(request.getFornavn() != null);
    Validate.isTrue(request.getEtternavn() != null);
    if (navnDto.getMellomnavn() != null) {
      Validate.isTrue(navnDto.getMellomnavn().equals(request.getMellomnavn()));
    } else {
      Validate.isTrue(request.getMellomnavn() == null);
    }

    navnekontroll(request, navnDto);

    log.info("Sjekk av oppgitt fars fødselsnummer, navn, og kjønn er gjennomført uten feil");
    return HttpResponse.from(HttpStatus.OK);
  }

  private void kontrollereKjoennFar(String foedselsnummer) {
    var kjoennFar = henteKjoenn(foedselsnummer).getResponseEntity().getBody();
    if (!Kjoenn.MANN.equals(kjoennFar)) {
      throw new FeilKjoennPaaOppgittFarException("Oppgitt far er ikke mann!");
    }
  }

  private NavnDto hentNavnTilPerson(KontrollerePersonopplysningerRequest request) {
    var responsMedNavn = pdlApiConsumer.hentNavnTilPerson(request.getFoedselsnummer());
    return responsMedNavn.getResponseEntity().getBody();
  }

  public HttpResponse<Void> oppretteFarskapserklaering(
      String fnrPaaloggetPerson, OppretteFarskaperklaeringRequest request) {
    var kjoennPaaloggetPerson = henteKjoenn(fnrPaaloggetPerson);
    var paaloggetPersonErMor =
        Kjoenn.KVINNE.equals(kjoennPaaloggetPerson.getResponseEntity().getBody());

    // Kontrollere opplysninger om far i request
    riktigNavnOgKjoennOppgittForFar(request.getOpplysningerOmFar());

    if (paaloggetPersonErMor
        && !fnrPaaloggetPerson.equals(request.getOpplysningerOmFar().getFoedselsnummer())) {
      var barn = BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
      if (request.getBarn().getFoedselsnummer() != null
          && !request.getBarn().getFoedselsnummer().isBlank()) {
        barn.setFoedselsnummer(request.getBarn().getFoedselsnummer());
      }

      var navnMor =
          pdlApiConsumer.hentNavnTilPerson(fnrPaaloggetPerson).getResponseEntity().getBody();

      var mor =
          ForelderDto.builder()
              .foedselsnummer(fnrPaaloggetPerson)
              .fornavn(navnMor.getFornavn())
              .mellomnavn(navnMor.getMellomnavn())
              .etternavn(navnMor.getEtternavn())
              .build();

      var far =
          ForelderDto.builder()
              .forelderRolle(ForelderRolle.FAR)
              .foedselsnummer(request.getOpplysningerOmFar().getFoedselsnummer())
              .fornavn(request.getOpplysningerOmFar().getFornavn())
              .mellomnavn(request.getOpplysningerOmFar().getMellomnavn())
              .etternavn(request.getOpplysningerOmFar().getEtternavn())
              .build();

      var farskapserklaeringDto =
          FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).build();

      log.info("Oppretter dokument for farskapserklæring");
      var dokumentDto = pdfGeneratorConsumer.genererePdf(farskapserklaeringDto);
      log.info("Mor signerer farskapserklæring");
      difiESignaturConsumer.signereDokument(dokumentDto, mor);
      farskapserklaeringDto.setSignertErklaering(dokumentDto);
      log.info("Lagre farskapserklæring med mors signatur");
      persistenceService.lagreFarskapserklaering(farskapserklaeringDto);
    }

    return HttpResponse.from(HttpStatus.OK);
  }

  public HttpResponse<Void> erklaereFarskap(String fnrPaaloggetPerson, BarnDto barnDto) {

    kontrollereKjoennFar(fnrPaaloggetPerson);

    log.info(
        "Henter ventende farskapserklæring som gjelder barn med en bestemt termindato eller fødselsnummer");
    var farskapserklaering =
        persistenceService
            .henteFarskapserklaeringForBarn(fnrPaaloggetPerson, ForelderRolle.FAR, barnDto)
            .orElseThrow(
                () ->
                    new FarskapserklaeringIkkeFunnetException(
                        "Fant ingen ventende farskapserklæring for far relatert til barn med termindato "
                            + barnDto.toString()));

    return HttpResponse.from(HttpStatus.OK);
  }

  private void navnekontroll(
      KontrollerePersonopplysningerRequest navnOppgitt, NavnDto navnFraRegister) {
    boolean fornavnStemmer = navnFraRegister.getFornavn().equals(navnOppgitt.getFornavn());
    boolean mellomnavnStemmer =
        navnFraRegister.getMellomnavn() == null
            ? navnOppgitt.getMellomnavn() == null
            : navnOppgitt.getMellomnavn().equals(navnFraRegister.getMellomnavn());
    boolean etternavnStemmer = navnFraRegister.getEtternavn().equals(navnOppgitt.getEtternavn());

    if (!fornavnStemmer || !mellomnavnStemmer || !etternavnStemmer) {
      Map<String, Boolean> navnesjekk = new HashMap<>();
      navnesjekk.put("fornavn", fornavnStemmer);
      navnesjekk.put("mellomnavn", mellomnavnStemmer);
      navnesjekk.put("etternavn", etternavnStemmer);

      StringBuffer sb = new StringBuffer();
      navnesjekk.forEach((k, v) -> leggeTil(!fornavnStemmer, k, sb));

      log.error("Navnekontroll feilet. Status navnesjekk (false = feilet): {}", navnesjekk);

      throw new OppgittNavnStemmerIkkeMedRegistrertNavnException(
          "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret");
    }

    log.info("Navnekontroll gjennomført uten feil");
  }

  private void leggeTil(boolean skalLeggesTil, String navnedel, StringBuffer sb) {
    if (skalLeggesTil) {
      sb.append(navnedel);
    }
  }
}
