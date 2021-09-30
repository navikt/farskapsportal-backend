package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.service.FarskapsportalService.KODE_LAND_NORGE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiException;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.consumer.pdl.api.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.consumer.pdl.api.ForelderBarnRelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.SivilstandDto;
import no.nav.farskapsportal.dto.NavnDto;
import no.nav.farskapsportal.exception.FeilNavnOppgittException;
import org.modelmapper.ModelMapper;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class PersonopplysningService {

  private static final String VERGE_OMFANG_PERSONLIGE_OG_OEKONOMISKE_INTERESSER = "personligeOgOekonomiskeInteresser";
  private static final String VERGE_OMFANG_PERSONLIGE_INTERESSER = "personligeInteresser";

  private final PdlApiConsumer pdlApiConsumer;

  private final FarskapsportalEgenskaper farskapsportalEgenskaper;

  private ModelMapper modelMapper;

  public Set<String> henteNyligFoedteBarnUtenRegistrertFar(String fnrMor) {
    Set<String> spedbarnUtenFar = new HashSet<>();
    List<ForelderBarnRelasjonDto> forelderBarnRelasjoner = pdlApiConsumer.henteForelderBarnRelasjon(fnrMor);

    var registrerteBarn = forelderBarnRelasjoner.stream().filter(Objects::nonNull)
        .filter(mor -> mor.getMinRolleForPerson().equals(ForelderBarnRelasjonRolle.MOR))
        .filter(barn -> barn.getRelatertPersonsRolle().equals(ForelderBarnRelasjonRolle.BARN)).map(ForelderBarnRelasjonDto::getRelatertPersonsIdent)
        .collect(Collectors.toSet());

    for (String fnrBarn : registrerteBarn) {
      var fd = henteFoedselsdato(fnrBarn);
      if (fd.isAfter(LocalDate.now().minusMonths(farskapsportalEgenskaper.getMaksAntallMaanederEtterFoedsel()))) {
        List<ForelderBarnRelasjonDto> spedbarnetsForelderBarnRelasjoner = pdlApiConsumer.henteForelderBarnRelasjon(fnrBarn);
        var spedbarnetsFarsrelasjon = spedbarnetsForelderBarnRelasjoner.stream()
            .filter(f -> f.getRelatertPersonsRolle().name().equals(Forelderrolle.FAR.toString())).findFirst();
        if (spedbarnetsFarsrelasjon.isEmpty()) {
          spedbarnUtenFar.add(fnrBarn);
        }
      }
    }

    // Barn må være født i Norge
    return filrereBortBarnFoedtUtenforNorge(spedbarnUtenFar);
  }

  public boolean erDoed(String foedselsnummer) {
    var doedsfallDto = pdlApiConsumer.henteDoedsfall(foedselsnummer);
    if (doedsfallDto == null) {
      return false;
    } else {
      log.warn("Person er registrert død den {}", doedsfallDto.getDoedsdato());
      return true;
    }
  }

  public boolean harNorskBostedsadresse(String foedselsnummer) {
    var bostedsadresseDto = pdlApiConsumer.henteBostedsadresse(foedselsnummer);
    if (bostedsadresseDto.getVegadresse() != null && bostedsadresseDto.getVegadresse().getAdressenavn() != null) {
      log.info("Personen er registrert med norsk vegadresse på postnummer: {}", bostedsadresseDto.getVegadresse().getPostnummer());
      return true;
    } else if (bostedsadresseDto.getMatrikkeladresse() != null && bostedsadresseDto.getMatrikkeladresse().getPostnummer() != null) {
      log.warn("Personen er ikke registrert med norsk vegadresse, men har matrikkeladresse, hentet ut postnummer {}",
          bostedsadresseDto.getMatrikkeladresse().getPostnummer());
      return true;
    } else if (bostedsadresseDto.getUtenlandskAdresse() != null) {
      var utenlandskAdresseDto = bostedsadresseDto.getUtenlandskAdresse();
      log.warn("Personen står oppført med utenlands adresse i PDL, landkode: {}", utenlandskAdresseDto.getLandkode());
    } else if (bostedsadresseDto.getUkjentBosted() != null) {
      var ukjentBostedDto = bostedsadresseDto.getUkjentBosted();
      log.warn("Personen står oppført med ukjent bosted i PDL, kommunenummer fra siste kjente bostedsadresse: {}",
          ukjentBostedDto.getBostedskommune());
    }
    return false;
  }

  public String henteFoedested(String foedselsnummer) {
    return pdlApiConsumer.henteFoedsel(foedselsnummer).getFoedested();
  }

  public LocalDate henteFoedselsdato(String foedselsnummer) {
    return pdlApiConsumer.henteFoedsel(foedselsnummer).getFoedselsdato();
  }

  public String henteFoedeland(String foedselsnummer) {
    return pdlApiConsumer.henteFoedsel(foedselsnummer).getFoedeland();
  }

  public boolean harVerge(String foedselsnummer) {
    return !pdlApiConsumer.henteVergeEllerFremtidsfullmakt(foedselsnummer).stream()
        .filter(Objects::nonNull)
        .filter(verge ->
            verge.getVergeEllerFullmektig().getOmfang().equalsIgnoreCase(VERGE_OMFANG_PERSONLIGE_OG_OEKONOMISKE_INTERESSER) ||
                verge.getVergeEllerFullmektig().getOmfang().equalsIgnoreCase(VERGE_OMFANG_PERSONLIGE_INTERESSER))
        .collect(Collectors.toList()).isEmpty();
  }

  public FolkeregisteridentifikatorDto henteFolkeregisteridentifikator(String foedselsnummer) {
    return pdlApiConsumer.henteFolkeregisteridentifikator(foedselsnummer);
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

  public KjoennDto henteGjeldendeKjoenn(String foedselsnummer) {
    return pdlApiConsumer.henteKjoennUtenHistorikk(foedselsnummer);
  }

  public NavnDto henteNavn(String foedselsnummer) {
    return modelMapper.map(pdlApiConsumer.hentNavnTilPerson(foedselsnummer), NavnDto.class);
  }

  public SivilstandDto henteSivilstand(String foedselsnummer) {
    return pdlApiConsumer.henteSivilstand(foedselsnummer);
  }

  public boolean erOver18Aar(String foedselsnummer) {
    var foedselsdato = henteFoedselsdato(foedselsnummer);
    if (LocalDate.now().minusYears(18).isBefore(foedselsdato)) {
      log.warn("Forelder med fødselsdato {}, er ikke myndig", foedselsdato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
      return false;
    }
    return true;
  }

  public void navnekontroll(String navn, NavnDto navnFraRegister) {

    var sammenslaattNavnFraRegister = navnFraRegister.getFornavn() + hentMellomnavnHvisFinnes(navnFraRegister) + navnFraRegister.getEtternavn();

    boolean navnStemmer = normalisereNavn(sammenslaattNavnFraRegister).equalsIgnoreCase(normalisereNavn(navn));

    if (!navnStemmer) {
      log.error(
          "Navnekontroll feilet. Navn stemmer ikke med navn registrert i folkeregisteret. Oppgitt navn: {} er forskjellig fra navn i register: {}",
          navn, sammenslaattNavnFraRegister);
      throw new FeilNavnOppgittException(navn, sammenslaattNavnFraRegister);
    }

    log.info("Navnekontroll gjennomført uten feil");
  }

  // Fjerner spesialtegn
  private String normalisereNavn(String navn) {
    return navn
        .replaceAll("é", "e")
        .replaceAll("\\s+", "");
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

  private String hentMellomnavnHvisFinnes(NavnDto navnFraRegister) {
    return navnFraRegister.getMellomnavn() == null || navnFraRegister.getMellomnavn().length() < 1 ? " "
        : " " + navnFraRegister.getMellomnavn() + " ";
  }

  private Set<String> filrereBortBarnFoedtUtenforNorge(Set<String> nyfoedteBarn) {
    return nyfoedteBarn.stream().filter(barn -> henteFoedeland(barn) != null).filter(barn -> henteFoedeland(barn).equalsIgnoreCase(KODE_LAND_NORGE))
        .collect(Collectors.toSet());
  }
}
