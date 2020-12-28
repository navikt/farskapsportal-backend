package no.nav.farskapsportal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiException;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennTypeDto;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.exception.FeilForelderrollePaaOppgittPersonException;
import no.nav.farskapsportal.exception.OppgittNavnStemmerIkkeMedRegistrertNavnException;
import no.nav.farskapsportal.exception.PersonHarFeilRolleException;
import org.apache.commons.lang3.Validate;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class PersonopplysningService {

  private static int MAKS_ALDER_I_MND_FOR_BARN_UTEN_FAR = 4;

  private final PdlApiConsumer pdlApiConsumer;

  public Set<String> henteNyligFoedteBarnUtenRegistrertFar(String fnrMor) {
    Set<String> spedbarnUtenFar = new HashSet<>();
    List<FamilierelasjonerDto> familierelasjoner = pdlApiConsumer.henteFamilierelasjoner(fnrMor);

    var registrerteBarn =
        familierelasjoner.stream()
            .filter(Objects::nonNull)
            .filter(mor -> mor.getMinRolleForPerson().equals(FamilierelasjonRolle.MOR))
            .filter(barn -> barn.getRelatertPersonsRolle().equals(FamilierelasjonRolle.BARN))
            .map(FamilierelasjonerDto::getRelatertPersonsIdent)
            .collect(Collectors.toSet());

    for (String fnrBarn : registrerteBarn) {
      var fd = henteFoedselsdato(fnrBarn);
      if (fd.isAfter(LocalDate.now().minusMonths(MAKS_ALDER_I_MND_FOR_BARN_UTEN_FAR))) {
        List<FamilierelasjonerDto> spedbarnetsFamilierelasjoner = pdlApiConsumer.henteFamilierelasjoner(fnrBarn);
        var spedbarnetsFarsrelasjon = spedbarnetsFamilierelasjoner.stream().filter(f -> f.getRelatertPersonsRolle().name().equals(Forelderrolle.FAR.toString())).findFirst();
        if (spedbarnetsFarsrelasjon.isEmpty()) {
          spedbarnUtenFar.add(fnrBarn);
        }
      }
    }

    return spedbarnUtenFar;
  }

  public LocalDate henteFoedselsdato(String foedselsnummer) {
    return pdlApiConsumer.henteFoedselsdato(foedselsnummer);
  }

  public Forelderrolle bestemmeForelderrolle(String foedselsnummer) {
    var gjeldendeKjoenn = henteGjeldendeKjoenn(foedselsnummer);

    if (KjoennTypeDto.UKJENT.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.UKJENT;
    }

    var kjoennshistorikk = pdlApiConsumer.henteKjoennMedHistorikk(foedselsnummer);
    var foedekjoenn = hentFoedekjoenn(kjoennshistorikk);

    // MOR -> Fødekjønn == kvinne && gjeldende kjønn == kvinne
    if (KjoennTypeDto.KVINNE.equals(foedekjoenn.getKjoenn())
        && KjoennTypeDto.KVINNE.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.MOR;
    }

    // MOR_ELLER_FAR -> Fødekjønn == kvinne && gjeldende kjønn == mann
    if (KjoennTypeDto.KVINNE.equals(foedekjoenn.getKjoenn())
        && KjoennTypeDto.MANN.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.MOR_ELLER_FAR;
    }

    // MEDMOR -> Fødekjønn == mann && gjeldende kjønn == kvinne
    if (KjoennTypeDto.MANN.equals(foedekjoenn.getKjoenn())
        && KjoennTypeDto.KVINNE.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.MEDMOR;
    }

    return Kjoenn.KVINNE.equals(gjeldendeKjoenn) ? Forelderrolle.MOR : Forelderrolle.FAR;
  }

  private no.nav.farskapsportal.consumer.pdl.api.KjoennDto hentFoedekjoenn(
      List<no.nav.farskapsportal.consumer.pdl.api.KjoennDto> kjoennshistorikk) {

    if (kjoennshistorikk.size() == 1) {
      return kjoennshistorikk.get(0);
    }

    var minsteGyldighetstidspunkt =
        kjoennshistorikk.stream()
            .map(kjoennDto -> kjoennDto.getFolkeregistermetadata().getGyldighetstidspunkt())
            .min(LocalDateTime::compareTo)
            .orElseThrow(
                () ->
                    new PdlApiException(
                        "Feil ved henting av laveste gyldighetstidspunkt for kjønnshistorikk"));
    return kjoennshistorikk.stream()
        .filter(
            kjoennDto ->
                kjoennDto
                    .getFolkeregistermetadata()
                    .getGyldighetstidspunkt()
                    .equals(minsteGyldighetstidspunkt))
        .findFirst()
        .orElseThrow(() -> new PdlApiException("Feil ved henting av originalt kjønn"));
  }

  public KjoennDto henteGjeldendeKjoenn(String foedselsnummer) {
    return pdlApiConsumer.henteKjoennUtenHistorikk(foedselsnummer);
  }

  public NavnDto henteNavn(String foedselsnummer) {
    return pdlApiConsumer.hentNavnTilPerson(foedselsnummer);
  }

  private NavnDto henteNavn(KontrollerePersonopplysningerRequest request) {
    return henteNavn(request.getFoedselsnummer());
  }

  public void riktigNavnOgRolle(
      KontrollerePersonopplysningerRequest request, Forelderrolle paakrevdForelderrolle) {
    Validate.isTrue(request.getFoedselsnummer() != null);

    var faktiskForelderrolle = bestemmeForelderrolle(request.getFoedselsnummer());

    if (!paakrevdForelderrolle.equals(faktiskForelderrolle)) {
      throw new FeilForelderrollePaaOppgittPersonException(
          "Forventet forelderrolle: "
              + paakrevdForelderrolle
              + ", faktisk forelderrolle: "
              + faktiskForelderrolle);
    }

    NavnDto navnDto = henteNavn(request);

    // Validere input
    Validate.isTrue(request.getNavn() != null);
    navnekontroll(request, navnDto);
    log.info("Sjekk av oppgitt fars fødselsnummer, navn, og kjønn er gjennomført uten feil");
  }

  public void kanOpptreSomMor(String fnrPaaloggetPerson) {
    var kjoennPaaloggetPerson = bestemmeForelderrolle(fnrPaaloggetPerson);
    var paaloggetPersonKanOpptreSomMor =
        Forelderrolle.MOR.equals(kjoennPaaloggetPerson)
            || Forelderrolle.MOR_ELLER_FAR.equals(kjoennPaaloggetPerson);

    if (!paaloggetPersonKanOpptreSomMor) {
      throw new PersonHarFeilRolleException(
          "Pålogget person er ikke mor! Bare mor kan starte signeringsprosessen...");
    }
  }

  private void kontrollereAtMorOgFarIkkeErSammePerson(String fnrMor, String fnrFar) {
    if (fnrMor.equals(fnrFar)) {
      throw new PersonHarFeilRolleException("Mor og far kan ikke være samme person!");
    }
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
