package no.nav.farskapsportal.backend.apps.api.config;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;
import static no.nav.farskapsportal.backend.libs.felles.config.RestTemplateFellesConfig.X_API_KEY;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplication;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalApiEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.backend.apps.api.model.Skriftspraak;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.apps.api.service.Mapper;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.sts.SecurityTokenServiceConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.apache.commons.lang3.Validate;
import org.flywaydb.core.Flyway;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
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
@ComponentScan("no.nav.farskapsportal.backend")
public class FarskapsportalApiConfig {

  private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

  private static final String BEHANDLINGSNUMMER = "Behandlingsnummer";
  private static final String BEHANDLINGSNUMMER_FARSKAP = "B145";
  private static final String TEMA = "Tema";
  private static final String TEMA_FAR = "FAR";

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-key", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
        ).info(new io.swagger.v3.oas.models.info.Info().title("farskapsportal-api").version("v1"));
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

  @Bean("pdl-api")
  @Scope("prototype")
  public HttpHeaderRestTemplate pdlApiRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${APIKEY_PDLAPI_FP}") String xApiKeyPdlApi,
      @Autowired SecurityTokenServiceConsumer securityTokenServiceConsumer) {

    httpHeaderRestTemplate.addHeaderGenerator(AUTHORIZATION,
        () -> "Bearer " + securityTokenServiceConsumer.hentIdTokenForServicebruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn(),
            farskapsportalFellesEgenskaper.getSystembrukerPassord()));

    httpHeaderRestTemplate.addHeaderGenerator(NAV_CONSUMER_TOKEN,
        () -> "Bearer " + securityTokenServiceConsumer.hentIdTokenForServicebruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn(),
            farskapsportalFellesEgenskaper.getSystembrukerPassord()));

    httpHeaderRestTemplate.addHeaderGenerator(BEHANDLINGSNUMMER, () -> BEHANDLINGSNUMMER_FARSKAP);
    httpHeaderRestTemplate.addHeaderGenerator(TEMA, () -> TEMA_FAR);

    log.info("Setter {} for pdl-api", X_API_KEY);
    Validate.isTrue(xApiKeyPdlApi != null);
    Validate.isTrue(!xApiKeyPdlApi.isBlank());

    httpHeaderRestTemplate.addHeaderGenerator(X_API_KEY, () -> xApiKeyPdlApi);
    return httpHeaderRestTemplate;
  }

  @Bean
  public PersonopplysningService personopplysningService(ModelMapper modelMapper, PdlApiConsumer pdlApiConsumer,
      FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper) {
    return PersonopplysningService.builder()
        .modelMapper(modelMapper)
        .pdlApiConsumer(pdlApiConsumer)
        .farskapsportalFellesEgenskaper(farskapsportalFellesEgenskaper).build();
  }

  @Bean
  public FarskapsportalService farskapsportalService(BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      FarskapsportalApiEgenskaper farskapsportalApiEgenskaper,
      DifiESignaturConsumer difiESignaturConsumer,
      PdfGeneratorConsumer pdfGeneratorConsumer,
      PersistenceService persistenceService,
      PersonopplysningService personopplysningService,
      Mapper mapper) {

    return FarskapsportalService.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalApiEgenskaper(farskapsportalApiEgenskaper)
        .difiESignaturConsumer(difiESignaturConsumer)
        .pdfGeneratorConsumer(pdfGeneratorConsumer)
        .persistenceService(persistenceService)
        .personopplysningService(personopplysningService)
        .mapper(mapper).build();
  }

  @Bean
  public ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(FarskapsportalApiConfig.class.getSimpleName());
  }

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  public OidcTokenManager oidcTokenManager(TokenValidationContextHolder tokenValidationContextHolder) {
    return () -> Optional.ofNullable(tokenValidationContextHolder).map(TokenValidationContextHolder::getTokenValidationContext)
        .map(tokenValidationContext -> tokenValidationContext.getJwtTokenAsOptional(FarskapsportalApiApplication.ISSUER_SELVBETJENING))
        .map(Optional::get)
        .map(JwtToken::getTokenAsString)
        .orElseThrow(() -> new IllegalStateException("Kunne ikke videresende Bearer token"));
  }

  @Bean
  public OidcTokenSubjectExtractor oidcTokenSubjectExtractor(OidcTokenManager oidcTokenManager) {
    return () -> henteIdentFraToken(oidcTokenManager);
  }

  private String henteIdentFraToken(OidcTokenManager oidcTokenManager) {
    var ident = SecurityUtils.hentePid(oidcTokenManager.hentIdToken());
    return erNumerisk(ident) ? ident : SecurityUtils.henteSubject(oidcTokenManager.hentIdToken());
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
  @Profile(PROFILE_LIVE)
  public static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource, @Value("${spring.flyway.placeholders.user}") String dbUserAsynkron)
        throws InterruptedException {
      Thread.sleep(30000);
      var placeholders = new HashMap<String, String>();
      placeholders.put("user_asynkron", dbUserAsynkron);

      Flyway.configure().dataSource(dataSource).placeholders(placeholders).load().migrate();
    }
  }

  public static class StringToEnumConverter implements Converter<String, Skriftspraak> {

    @Override
    public Skriftspraak convert(String source) {
      return Skriftspraak.valueOf(source.toUpperCase());
    }
  }

  @Configuration
  public static class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
      registry.addConverter(new StringToEnumConverter());
    }
  }

  public static boolean erNumerisk(String ident) {
    try {
      Long.parseLong(ident);
      log.info("Identen er numerisk");
      return true;
    } catch (NumberFormatException e) {
      log.warn("Identen er ikke numerisk");
      return false;
    }
  }
}
