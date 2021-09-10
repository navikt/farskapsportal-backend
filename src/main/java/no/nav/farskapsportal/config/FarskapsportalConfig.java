package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;
import static no.nav.farskapsportal.consumer.sts.SecurityTokenServiceEndpointName.HENTE_IDTOKEN_FOR_SERVICEUSER;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import no.nav.farskapsportal.api.Skriftspraak;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.consumer.sts.SecurityTokenServiceConsumer;
import no.nav.farskapsportal.gcp.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.gcp.secretmanager.FarskapKeystoreCredentials;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.service.FarskapsportalService;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.service.PersonopplysningService;
import no.nav.farskapsportal.util.Mapper;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.flywaydb.core.Flyway;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "farskapsportal-api", version = "v1"),
    security = @SecurityRequirement(name = "bearer-key")
)
public class FarskapsportalConfig {

  public static final String X_API_KEY = "x-nav-apiKey";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-key", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
        ).info(new io.swagger.v3.oas.models.info.Info().title("farskapsportal-api").version("v1"));
  }

  @Bean
  @Profile({PROFILE_LIVE, PROFILE_INTEGRATION_TEST})
  public KeyStoreConfig keyStoreConfig(
      @Value("${virksomhetssertifikat.prosjektid}") String virksomhetssertifikatProsjektid,
      @Value("${virksomhetssertifikat.hemmelighetnavn}") String virksomhetssertifikatHemmelighetNavn,
      @Value("${virksomhetssertifikat.hemmelighetversjon}") String virksomhetssertifikatHemmelighetVersjon,
      @Value("${virksomhetssertifikat.passord.prosjektid}") String virksomhetssertifikatPassordProsjektid,
      @Value("${virksomhetssertifikat.passord.hemmelighetnavn}") String virksomhetssertifikatPassordHemmelighetNavn,
      @Value("${virksomhetssertifikat.passord.hemmelighetversjon}") String virksomhetssertifikatPassordHemmelighetVersjon,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion) throws IOException {

    var sertifikatpassord = accessSecretVersion
        .accessSecretVersion(virksomhetssertifikatPassordProsjektid, virksomhetssertifikatPassordHemmelighetNavn,
            virksomhetssertifikatPassordHemmelighetVersjon).getData().toStringUtf8();

    var objectMapper = new ObjectMapper();
    var farskapKeystoreCredentials = objectMapper.readValue(sertifikatpassord, FarskapKeystoreCredentials.class);

    log.info("lengde sertifikatpassord {}", farskapKeystoreCredentials.getPassword().length());

    var secretPayload = accessSecretVersion
        .accessSecretVersion(virksomhetssertifikatProsjektid, virksomhetssertifikatHemmelighetNavn, virksomhetssertifikatHemmelighetVersjon);

    log.info("lengde sertifikat: {}", secretPayload.getData().size());
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig
        .fromJavaKeyStore(inputStream, farskapKeystoreCredentials.getAlias(), farskapKeystoreCredentials.getPassword(),
            farskapKeystoreCredentials.getPassword());
  }

  @Bean
  public PdlApiConsumer pdlApiConsumer(@Qualifier("pdl-api") RestTemplate restTemplate,
      @Value("${url.pdl-api.base-url}") String baseUrl,
      @Value("${url.pdl-api.graphql}") String pdlApiEndpoint,
      ConsumerEndpoint consumerEndpoint) {
    consumerEndpoint.addEndpoint(PdlApiConsumerEndpointName.PDL_API_GRAPHQL, pdlApiEndpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    log.info("Oppretter PdlApiConsumer med url {}", baseUrl);
    return PdlApiConsumer.builder().restTemplate(restTemplate).consumerEndpoint(consumerEndpoint).build();
  }

  @Bean
  SecurityTokenServiceConsumer securityTokenServiceConsumer(@Qualifier("sts") RestTemplate restTemplate,
      @Value("${url.sts.base-url}") String baseUrl,
      @Value("${url.sts.security-token-service}") String endpoint,
      ConsumerEndpoint consumerEndpoint) {
    log.info("Oppretter SecurityTokenServiceConsumer med url {}", baseUrl);
    consumerEndpoint.addEndpoint(HENTE_IDTOKEN_FOR_SERVICEUSER, endpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    return new SecurityTokenServiceConsumer(restTemplate, consumerEndpoint);
  }

  @Bean
  public PersistenceService persistenceService(PersonopplysningService personopplysningService,
      FarskapserklaeringDao farskapserklaeringDao,
      Mapper mapper,
      BarnDao barnDao,
      ForelderDao forelderDao,
      StatusKontrollereFarDao kontrollereFarDao,
      MeldingsloggDao meldingsloggDao) {
    return new PersistenceService(personopplysningService, farskapserklaeringDao, barnDao, forelderDao, kontrollereFarDao,
        meldingsloggDao, mapper);
  }

  @Bean
  public PersonopplysningService personopplysningService(PdlApiConsumer pdlApiConsumer,
      FarskapsportalEgenskaper farskapsportalEgenskaper) {
    return PersonopplysningService.builder().pdlApiConsumer(pdlApiConsumer).farskapsportalEgenskaper(farskapsportalEgenskaper).build();
  }

  @Bean
  public FarskapsportalService farskapsportalService(BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      FarskapsportalEgenskaper farskapsportalEgenskaper,
      DifiESignaturConsumer difiESignaturConsumer,
      PdfGeneratorConsumer pdfGeneratorConsumer,
      PersistenceService persistenceService,
      PersonopplysningService personopplysningService,
      Mapper mapper) {

    return FarskapsportalService.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalEgenskaper(farskapsportalEgenskaper)
        .difiESignaturConsumer(difiESignaturConsumer)
        .pdfGeneratorConsumer(pdfGeneratorConsumer)
        .persistenceService(persistenceService)
        .personopplysningService(personopplysningService)
        .mapper(mapper).build();
  }

  @Bean
  public ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(FarskapsportalConfig.class.getSimpleName());
  }

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  public OidcTokenManager oidcTokenManager(TokenValidationContextHolder tokenValidationContextHolder) {
    return () -> Optional.ofNullable(tokenValidationContextHolder).map(TokenValidationContextHolder::getTokenValidationContext)
        .map(tokenValidationContext -> tokenValidationContext.getJwtTokenAsOptional(ISSUER)).map(Optional::get).map(JwtToken::getTokenAsString)
        .orElseThrow(() -> new IllegalStateException("Kunne ikke videresende Bearer token"));
  }

  @Bean
  public OidcTokenSubjectExtractor oidcTokenSubjectExtractor(OidcTokenManager oidcTokenManager) {
    return () -> SecurityUtils.henteSubject(oidcTokenManager.hentIdToken());
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @FunctionalInterface
  public interface OidcTokenManager {

    String hentIdToken();
  }

  @FunctionalInterface
  public interface OidcTokenSubjectExtractor {

    String hentPaaloggetPerson();
  }

  @Configuration
  @Profile({PROFILE_LIVE})
  public static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource) throws InterruptedException {
      Thread.sleep(30000);
      Flyway.configure().dataSource(dataSource).load().migrate();
    }
  }

  public class StringToEnumConverter implements Converter<String, Skriftspraak> {

    @Override
    public Skriftspraak convert(String source) {
      return Skriftspraak.valueOf(source.toUpperCase());
    }
  }

  @Configuration
  public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
      registry.addConverter(new StringToEnumConverter());
    }
  }
}
