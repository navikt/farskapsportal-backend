package no.nav.farskapsportal.service;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.exception.FeilKjoennPaaOppgittFarException;
import no.nav.farskapsportal.exception.OppgittNavnStemmerIkkeMedRegistrertNavnException;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;

@Builder
@Slf4j
public class FarskapsportalService {

  public static final String FEIL_NAVN =
      "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret";
  private final PdlApiConsumer pdlApiConsumer;

  public HttpResponse<Kjoenn> henteKjoenn(String foedselsnummer) {
    return pdlApiConsumer.henteKjoenn(foedselsnummer);
  }

  public HttpResponse<Boolean> riktigNavnOppgittForFar(
      KontrollerePersonopplysningerRequest request) {
    Validate.isTrue(request.getFoedselsnummer() != null);

    var responsMedNavn = pdlApiConsumer.hentNavnTilPerson(request.getFoedselsnummer());
    var navnDto = responsMedNavn.getResponseEntity().getBody();

    // Validere input
    Validate.isTrue(request.getFornavn() != null);
    Validate.isTrue(request.getEtternavn() != null);
    if (navnDto.getMellomnavn() != null) {
      Validate.isTrue(navnDto.getMellomnavn().equals(request.getMellomnavn()));
    } else {
      Validate.isTrue(request.getMellomnavn() == null);
    }

    navnekontroll(request, navnDto);

    var kjoennFar = henteKjoenn(request.getFoedselsnummer()).getResponseEntity().getBody();
    if (!Kjoenn.MANN.equals(kjoennFar)) {
      throw new FeilKjoennPaaOppgittFarException("Oppgitt far er ikke mann!");
    }

    return HttpResponse.from(HttpStatus.OK, true);
  }

  private void navnekontroll(
      KontrollerePersonopplysningerRequest navnOppgitt, NavnDto navnFraRegister) {
    boolean fornavnStemmer = navnFraRegister.getFornavn().equals(navnOppgitt.getFornavn());
    boolean mellomnavnStemmer =
        navnFraRegister.getMellomnavn() == null
            ? navnOppgitt.getMellomnavn() == null
            : navnOppgitt.getMellomnavn().equals(navnOppgitt.getMellomnavn());
    boolean etternavnStemmer = navnFraRegister.getEtternavn().equals(navnOppgitt.getEtternavn());

    if (!fornavnStemmer || !mellomnavnStemmer || !etternavnStemmer) {
      Map<String, Boolean> navnesjekk = new HashMap<>();
      navnesjekk.put("fornavn", fornavnStemmer);
      navnesjekk.put("mellomnavn", mellomnavnStemmer);
      navnesjekk.put("etternavn", etternavnStemmer);

      StringBuffer sb = new StringBuffer();
      navnesjekk.forEach((k, v) -> leggeTil(!fornavnStemmer, k, sb));

      log.error("Navnekontroll feilet, {}, ");
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
