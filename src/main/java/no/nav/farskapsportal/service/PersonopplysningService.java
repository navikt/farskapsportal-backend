package no.nav.farskapsportal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiException;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.api.SivilstandDto;
import no.nav.farskapsportal.exception.ValideringException;
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

    var registrerteBarn = familierelasjoner.stream().filter(Objects::nonNull)
        .filter(mor -> mor.getMinRolleForPerson().equals(FamilierelasjonRolle.MOR))
        .filter(barn -> barn.getRelatertPersonsRolle().equals(FamilierelasjonRolle.BARN)).map(FamilierelasjonerDto::getRelatertPersonsIdent)
        .collect(Collectors.toSet());

    for (String fnrBarn : registrerteBarn) {
      var fd = henteFoedselsdato(fnrBarn);
      if (fd.isAfter(LocalDate.now().minusMonths(MAKS_ALDER_I_MND_FOR_BARN_UTEN_FAR))) {
        List<FamilierelasjonerDto> spedbarnetsFamilierelasjoner = pdlApiConsumer.henteFamilierelasjoner(fnrBarn);
        var spedbarnetsFarsrelasjon = spedbarnetsFamilierelasjoner.stream()
            .filter(f -> f.getRelatertPersonsRolle().name().equals(Forelderrolle.FAR.toString())).findFirst();
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
    log.info("Personens gjeldende kjønn: {}", gjeldendeKjoenn.getKjoenn().toString());
    if (KjoennType.UKJENT.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.UKJENT;
    }

    var kjoennshistorikk = pdlApiConsumer.henteKjoennMedHistorikk(foedselsnummer);
    var foedekjoenn = hentFoedekjoenn(kjoennshistorikk);

    // MOR -> Fødekjønn == kvinne && gjeldende kjønn == kvinne
    if (KjoennType.KVINNE.equals(foedekjoenn.getKjoenn()) && KjoennType.KVINNE.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.MOR;
    }

    // MOR_ELLER_FAR -> Fødekjønn == kvinne && gjeldende kjønn == mann
    if (KjoennType.KVINNE.equals(foedekjoenn.getKjoenn()) && KjoennType.MANN.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.MOR_ELLER_FAR;
    }

    // MEDMOR -> Fødekjønn == mann && gjeldende kjønn == kvinne
    // TODO: Undersøke om person med fødekjønn mann, men med gjeldende kjønn kvinn kan opptre som far
    if (KjoennType.MANN.equals(foedekjoenn.getKjoenn()) && KjoennType.KVINNE.equals(gjeldendeKjoenn.getKjoenn())) {
      return Forelderrolle.MEDMOR;
    }

    return Kjoenn.KVINNE.equals(gjeldendeKjoenn) ? Forelderrolle.MOR : Forelderrolle.FAR;
  }

  private no.nav.farskapsportal.consumer.pdl.api.KjoennDto hentFoedekjoenn(List<no.nav.farskapsportal.consumer.pdl.api.KjoennDto> kjoennshistorikk) {

    if (kjoennshistorikk.size() == 1) {
      return kjoennshistorikk.get(0);
    }

    var minsteGyldighetstidspunkt = kjoennshistorikk.stream().map(kjoennDto -> kjoennDto.getFolkeregistermetadata().getGyldighetstidspunkt())
        .min(LocalDateTime::compareTo).orElseThrow(() -> new PdlApiException(Feilkode.PDL_KJOENN_LAVESTE_GYLDIGHETSTIDSPUNKT));
    return kjoennshistorikk.stream()
        .filter(kjoennDto -> kjoennDto.getFolkeregistermetadata().getGyldighetstidspunkt().equals(minsteGyldighetstidspunkt)).findFirst()
        .orElseThrow(() -> new PdlApiException(Feilkode.PDL_KJOENN_ORIGINALT));
  }

  public KjoennDto henteGjeldendeKjoenn(String foedselsnummer) {
    return pdlApiConsumer.henteKjoennUtenHistorikk(foedselsnummer);
  }

  public NavnDto henteNavn(String foedselsnummer) {
    return pdlApiConsumer.hentNavnTilPerson(foedselsnummer);
  }

  public SivilstandDto henteSivilstand(String foedselsnummer) {
    return pdlApiConsumer.henteSivilstand(foedselsnummer);
  }

  public void riktigNavnRolleFar(String foedselsnummer, String navn) {

    var feilkode =
        foedselsnummer == null || foedselsnummer.trim().length() < 1 ? Optional.of(Feilkode.FOEDSELNUMMER_MANGLER_FAR) : Optional.<Feilkode>empty();
    feilkode = navn == null || navn.trim().length() < 1 ? Optional.of(Feilkode.FOEDSELNUMMER_MANGLER_FAR) : feilkode;

    feilkode = !Forelderrolle.FAR.equals(bestemmeForelderrolle(foedselsnummer)) ? Optional.of(Feilkode.FEIL_ROLLE_FAR) : feilkode;

    if (feilkode.isPresent()) {
      throw new ValideringException(feilkode.get());
    }

    NavnDto navnDtoFraFolkeregisteret = henteNavn(foedselsnummer);

    // Validere input
    navnekontroll(navn, navnDtoFraFolkeregisteret);

    // Far må være myndig
    erMyndig(foedselsnummer);

    log.info("Sjekk av oppgitt fars fødselsnummer, navn, og kjønn er gjennomført uten feil");
  }

  public void erMyndig(String foedselsnummer) {
    var foedselsdato = henteFoedselsdato(foedselsnummer);
    if (LocalDate.now().minusYears(18).isBefore(foedselsdato)) {
      throw new ValideringException(Feilkode.IKKE_MYNDIG);
    }
  }

  private void navnekontroll(String navn, NavnDto navnFraRegister) {

    var sammenslaattNavnFraRegister = navnFraRegister.getFornavn() + hentMellomnavnHvisFinnes(navnFraRegister) + navnFraRegister.getEtternavn();

    boolean navnStemmer = sammenslaattNavnFraRegister.equalsIgnoreCase(navn.replaceAll("\\s+", ""));

    if (!navnStemmer) {
      log.error("Navnekontroll feilet. Navn stemmer ikke med navn registrert i folkeregisteret");
      throw new ValideringException(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER);
    }

    log.info("Navnekontroll gjennomført uten feil");
  }

  private String hentMellomnavnHvisFinnes(NavnDto navnFraRegister) {
    return navnFraRegister.getMellomnavn() == null || navnFraRegister.getMellomnavn().length() < 1 ? "" : navnFraRegister.getMellomnavn();
  }
}
