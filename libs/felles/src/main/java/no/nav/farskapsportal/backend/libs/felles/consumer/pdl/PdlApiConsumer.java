package no.nav.farskapsportal.backend.libs.felles.consumer.pdl;

import static java.util.stream.Collectors.toList;
import static no.nav.farskapsportal.backend.libs.felles.consumer.pdl.PdlApiConsumerEndpointName.PDL_API_GRAPHQL;
import static no.nav.farskapsportal.backend.libs.felles.util.Utils.toSingletonOrThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.dto.pdl.DoedsfallDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.FoedselDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.PdlDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.SivilstandDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.VergemaalEllerFremtidsfullmaktDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.backend.libs.felles.consumer.pdl.graphql.GraphQLRequest;
import no.nav.farskapsportal.backend.libs.felles.consumer.pdl.graphql.GraphQLResponse;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.UnrecoverableException;
import no.nav.farskapsportal.backend.libs.felles.exception.ValideringException;
import org.apache.commons.lang3.Validate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Builder
public class PdlApiConsumer {

  public static final String PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK = "I_BRUK";
  public static final String PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR = "FNR";
  private static final String MASTER_PDL = "PDL";
  private static final String MASTER_FREG = "FREG";

  @NonNull
  private final RestTemplate restTemplate;
  @NonNull
  private final ConsumerEndpoint consumerEndpoint;

  private static Predicate<PdlDto> isMasterPdlOrFreg() {
    return dto ->
        MASTER_PDL.equalsIgnoreCase(dto.getMetadata().getMaster())
            || MASTER_FREG.equalsIgnoreCase(dto.getMetadata().getMaster());
  }

  private static boolean isMasterPdlOrFreg(PdlDto dto) {
    return MASTER_PDL.equalsIgnoreCase(dto.getMetadata().getMaster())
        || MASTER_FREG.equalsIgnoreCase(dto.getMetadata().getMaster());
  }

  @Cacheable("bostedsadresse")
  public BostedsadresseDto henteBostedsadresse(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_BOSTEDSADRESSE, false);
    var bostedsadresseDtos = respons.getData().getHentPerson().getBostedsadresse();
    var bostedsadresseFraPdlEllerFreg = bostedsadresseDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (bostedsadresseFraPdlEllerFreg.isEmpty()) {
      throw new RessursIkkeFunnetException(Feilkode.PDL_BOSTEDSADRESSE_MANGLER);
    }
    return bostedsadresseFraPdlEllerFreg.stream().findFirst()
        .orElseThrow(() -> new PdlApiException(Feilkode.PDL_FOEDSELSDATO_TEKNISK_FEIL));
  }

  @Cacheable("doedsfall")
  public DoedsfallDto henteDoedsfall(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_DOEDSFALL, false);
    var doedsfallDto = respons.getData().getHentPerson().getDoedsfall();

    if (doedsfallDto.isEmpty() || !isMasterPdlOrFreg(doedsfallDto.get(0))) {
      return null;
    } else {
      return doedsfallDto.get(0);
    }
  }

  @Cacheable("foedsel")
  public FoedselDto henteFoedsel(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_FOEDSEL, false);
    var foedselDtos = respons.getData().getHentPerson().getFoedsel();

    var foedselDtosFraPdlEllerFreg = foedselDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (foedselDtosFraPdlEllerFreg.isEmpty()) {
      throw new RessursIkkeFunnetException(Feilkode.PDL_FOEDSELSDATO_MANGLER);
    }

    return foedselDtosFraPdlEllerFreg.stream().findFirst().orElseThrow(() -> new PdlApiException(Feilkode.PDL_FOEDSELSDATO_TEKNISK_FEIL));
  }

  @Cacheable("folkeregisteridentifikator")
  public FolkeregisteridentifikatorDto henteFolkeregisteridentifikator(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_FOLKEREGISTERIDENTIFIKATOR, false);
    var folkeregisteridentifikatorDtos = respons.getData().getHentPerson().getFolkeregisteridentifikator();

    var folkeregisteridentifikatorDtosFraFregEllerPdl = folkeregisteridentifikatorDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (folkeregisteridentifikatorDtosFraFregEllerPdl.isEmpty()) {
      throw new RessursIkkeFunnetException(Feilkode.PDL_FOLKEREGISTERIDENTIFIKATOR_IKKE_FUNNET);
    }

    return folkeregisteridentifikatorDtosFraFregEllerPdl.stream().filter(Objects::nonNull)
        .collect(toSingletonOrThrow(new UnrecoverableException(
            "Feil ved mapping av folkeregisteridentifikator, forventet bare et innslag av folkeregisteridentifikator på person")));
  }

  @Cacheable("forelderBarnRelasjon")
  public List<ForelderBarnRelasjonDto> henteForelderBarnRelasjon(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_FORELDER_BARN_RELASJON, false);
    var forelderBarnRelasjonDtos = respons.getData().getHentPerson().getForelderBarnRelasjon();
    return forelderBarnRelasjonDtos.stream().filter(Objects::nonNull).filter(isMasterPdlOrFreg()).collect(toList());
  }

  @Cacheable("kjoenn")
  public KjoennDto henteKjoennUtenHistorikk(String foedselsnummer) {

    var kjoennFraPdlEllerFreg = henteKjoenn(foedselsnummer, false);

    return kjoennFraPdlEllerFreg.stream().filter(Objects::nonNull)
        .collect(toSingletonOrThrow(new UnrecoverableException("Feil ved mapping av kjønn, forventet bare et registrert kjønn på person")));
  }

  @Cacheable("kjoennshistorikk")
  public List<KjoennDto> henteKjoennMedHistorikk(String foedselsnummer) {
    var kjoennshistorikk = henteKjoenn(foedselsnummer, true);

    return kjoennshistorikk.stream().filter(Objects::nonNull).collect(toList());
  }

  @NotNull
  @Cacheable("navn")
  public NavnDto hentNavnTilPerson(String foedselsnummer) {
    log.info("Henter navn til person");
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_NAVN, false);
    var navnDtos = respons.getData().getHentPerson().getNavn();

    var navnFraPdlEllerFreg = navnDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (navnFraPdlEllerFreg.isEmpty()) {
      throw new RessursIkkeFunnetException(Feilkode.PDL_NAVN_IKKE_FUNNET);
    }

    var navnDto = navnFraPdlEllerFreg.stream().filter(Objects::nonNull)
        .collect(toSingletonOrThrow(new UnrecoverableException("Feil ved mapping av kjønn, forventet bare et registrert kjønn på person")));

    Validate.notNull(navnDto.getFornavn(), "Fornavn mangler i retur fra PDL!");
    Validate.notNull(navnDto.getEtternavn(), "Etternavn mangler i retur fra PDL!");

    return navnDto;
  }

  @NotNull
  @Cacheable("sivilstand")
  public SivilstandDto henteSivilstand(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_SIVILSTAND, false);
    var sivilstandDtos = respons.getData().getHentPerson().getSivilstand();

    var sivilstandFraPdlEllerFreg = sivilstandDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (sivilstandFraPdlEllerFreg.isEmpty()) {
      throw new RessursIkkeFunnetException(Feilkode.PDL_SIVILSTAND_IKKE_FUNNET);
    }

    return sivilstandFraPdlEllerFreg.stream().filter(Objects::nonNull)
        .collect(toSingletonOrThrow(new UnrecoverableException("Feil ved mapping av sivilstand, forventet bare et innslag av sivilstand på person")));
  }

  @Cacheable("verge")
  public List<VergemaalEllerFremtidsfullmaktDto> henteVergeEllerFremtidsfullmakt(String foedselsnummer) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_VERGE, false);
    var vergemaalEllerFremtidsfullmaktDtos = respons.getData().getHentPerson().getVergemaalEllerFremtidsfullmakt();

    var vergemaalEllerFremtidsfullmaktDtosFraPdlEllerFreg = vergemaalEllerFremtidsfullmaktDtos.stream()
        .filter(Objects::nonNull)
        .filter(isMasterPdlOrFreg()).collect(Collectors.toList());

    if (vergemaalEllerFremtidsfullmaktDtosFraPdlEllerFreg.isEmpty() || !isMasterPdlOrFreg(vergemaalEllerFremtidsfullmaktDtosFraPdlEllerFreg.get(0))) {
      return new ArrayList<>();
    } else {
      return vergemaalEllerFremtidsfullmaktDtosFraPdlEllerFreg;
    }
  }

  private List<KjoennDto> henteKjoenn(String foedselsnummer, boolean inkludereHistorikk) {
    var respons = hentePersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_KJOENN, inkludereHistorikk);
    var kjoennDtos = respons.getData().getHentPerson().getKjoenn();
    var kjoennFraPdlEllerFreg = kjoennDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (kjoennFraPdlEllerFreg.isEmpty()) {
      throw new RessursIkkeFunnetException(Feilkode.PDL_KJOENN_INGEN_INFO);
    }

    return kjoennFraPdlEllerFreg;
  }

  @Retryable(value = Exception.class, backoff = @Backoff(delay = 500))
  private GraphQLResponse hentePersondokument(String ident, String query, boolean inkludereHistorikk) {
    val graphQlRequest = GraphQLRequest.builder().query(query).variables(Map.of("historikk", inkludereHistorikk, "ident", ident)).build();

    var endpoint = consumerEndpoint.retrieveEndpoint(PDL_API_GRAPHQL);
    GraphQLResponse response = null;
    try {
      response = restTemplate.postForEntity(endpoint, graphQlRequest, GraphQLResponse.class).getBody();
    } catch (HttpClientErrorException clientErrorException) {
      clientErrorException.printStackTrace();
      if (response == null) {
        throw new ValideringException(Feilkode.PDL_PERSON_IKKE_FUNNET);
      }
    } catch (Exception e) {
      // Håndterer evnt feil i checkForPdlApiErrors
      e.printStackTrace();
      if (response == null) {
        throw e;
      }
    }

    return checkForPdlApiErrors(response);
  }

  private GraphQLResponse checkForPdlApiErrors(GraphQLResponse response) {
    Optional.ofNullable(response).map(GraphQLResponse::getErrors).ifPresent(errorJsonNodes -> {
      List<PdlApiError> errors = errorJsonNodes.stream().map(
          jsonNode -> PdlApiError.builder().message(jsonNode.get("message").toString()).code(jsonNode.path("extensions").path("code").toString())
              .build()).collect(toList());

      for (PdlApiError error : errors) {
        if (error.getMessage().contains("Fant ikke person") && error.getCode().contains("not_found")) {
          throw new RessursIkkeFunnetException(Feilkode.PDL_PERSON_IKKE_FUNNET);
        }
      }
      throw new PdlApiErrorException(errors);
    });
    return response;
  }
}
