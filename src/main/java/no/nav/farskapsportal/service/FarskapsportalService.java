package no.nav.farskapsportal.service;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.exception.FeilKjoennPaaOppgittFarException;
import no.nav.farskapsportal.exception.OppgittNavnStemmerIkkeMedRegistrertNavnException;
import no.nav.farskapsportal.exception.PersonIkkeFunnetException;
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

  public HttpResponse<?> riktigNavnOppgittForFar(KontrollerePersonopplysningerRequest request) {
    Validate.isTrue(request.getFoedselsnummer() != null);

    var responsMedNavn = pdlApiConsumer.hentNavnTilPerson(request.getFoedselsnummer());
    var navnDto = responsMedNavn.getResponseEntity().getBody();

    if (navnDto == null) {
      throw new PersonIkkeFunnetException("Responsen fra PDL mangler informasjon om person");
    }

    // Validere input
    Validate.isTrue(request.getNavn() != null);

    navnekontroll(request, navnDto);

    var kjoennFar = henteKjoenn(request.getFoedselsnummer()).getResponseEntity().getBody();
    if (!Kjoenn.MANN.equals(kjoennFar)) {
      throw new FeilKjoennPaaOppgittFarException("Oppgitt far er ikke mann!");
    }

    log.info("Sjekk av oppgitt fars fødselsnummer, navn, og kjønn er gjennomført uten feil");

    return HttpResponse.from(HttpStatus.OK);
  }

  private void navnekontroll(
      KontrollerePersonopplysningerRequest navnOppgitt, NavnDto navnFraRegister) {

    var sammenslaattNavnFraRegister =
        navnFraRegister.getFornavn()
            + hentMellomnavnHvisFinnes(navnFraRegister)
            + navnFraRegister.getEtternavn();

    boolean navnStemmer =
        sammenslaattNavnFraRegister.equalsIgnoreCase(navnOppgitt.getNavn().replaceAll("\\s+", ""));

    if (!navnStemmer) {
      log.error("Navnekontroll feilet. Navn stemmer ikke med navn registrert i folkeregisteret");
      throw new OppgittNavnStemmerIkkeMedRegistrertNavnException(
          "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret");
    }

    log.info("Navnekontroll gjennomført uten feil");
  }

  private String hentMellomnavnHvisFinnes(NavnDto navnFraRegister) {
    return navnFraRegister.getMellomnavn() == null || navnFraRegister.getMellomnavn().length() < 1
        ? ""
        : navnFraRegister.getMellomnavn();
  }

  private void leggeTil(boolean skalLeggesTil, String navnedel, StringBuffer sb) {
    if (skalLeggesTil) {
      sb.append(navnedel);
    }
  }
}
