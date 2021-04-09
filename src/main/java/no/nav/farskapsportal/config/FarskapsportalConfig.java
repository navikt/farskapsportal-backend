package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;
import static no.nav.farskapsportal.consumer.skatt.SkattEndpointName.MOTTA_FARSKAPSERKLAERING;
import static no.nav.farskapsportal.consumer.sts.SecurityTokenServiceEndpointName.HENTE_IDTOKEN_FOR_SERVICEUSER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.consumer.pdl.PdlApiHelsesjekkConsumer;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.consumer.sts.SecurityTokenServiceConsumer;
import no.nav.farskapsportal.gcp.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.scheduled.OverfoereTilSkatt;
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
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class FarskapsportalConfig {

  public static final String X_API_KEY = "x-nav-apiKey";

  @Bean
  public ConsumerEndpoint consumerEndpoint() {
    return new ConsumerEndpoint();
  }

  @Bean
  SecurityTokenServiceConsumer securityTokenServiceConsumer(@Qualifier("sts") RestTemplate restTemplate, @Value("${url.sts.base-url}") String baseUrl,
      @Value("${url.sts.security-token-service}") String endpoint, ConsumerEndpoint consumerEndpoint) {
    log.info("Oppretter SecurityTokenServiceConsumer med url {}", baseUrl);
    consumerEndpoint.addEndpoint(HENTE_IDTOKEN_FOR_SERVICEUSER, endpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    return new SecurityTokenServiceConsumer(restTemplate, consumerEndpoint);
  }

  @Bean
  SkattConsumer skattConsumer(@Qualifier("skatt") RestTemplate restTemplate, @Value("${url.skatt.base-url}") String baseUrl,
      @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {
    log.info("Oppretter SkattConsumer med url {}", baseUrl);
    consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    return new SkattConsumer(restTemplate, consumerEndpoint);
  }

  @Bean
  public OverfoereTilSkatt overfoereTilSkatt(PersistenceService persistenceService, SkattConsumer skattConsumer) {
    return OverfoereTilSkatt.builder().persistenceService(persistenceService).skattConsumer(skattConsumer).build();
  }

  @Bean
  public PdlApiConsumer pdlApiConsumer(@Qualifier("pdl-api") RestTemplate restTemplate, @Value("${url.pdl-api.base-url}") String baseUrl,
      @Value("${url.pdl-api.graphql}") String pdlApiEndpoint, ConsumerEndpoint consumerEndpoint) {
    consumerEndpoint.addEndpoint(PdlApiConsumerEndpointName.PDL_API_GRAPHQL, pdlApiEndpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    log.info("Oppretter PdlApiConsumer med url {}", baseUrl);
    return PdlApiConsumer.builder().restTemplate(restTemplate).consumerEndpoint(consumerEndpoint).build();
  }

  @Bean
  public PdlApiHelsesjekkConsumer pdlApiHelsesjekkConsumer(@Qualifier("pdl-api") RestTemplate restTemplate,
      @Value("${url.pdl-api.base-url}") String baseUrl, @Value("${url.pdl-api.graphql}") String pdlApiEndpoint,
      ConsumerEndpoint consumerEndpoint) {
    consumerEndpoint.addEndpoint(PdlApiConsumerEndpointName.PDL_API_GRAPHQL, pdlApiEndpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    log.info("Oppretter PdlApiHelsesjekkConsumer med url {}", baseUrl);
    return PdlApiHelsesjekkConsumer.builder().restTemplate(restTemplate).consumerEndpoint(consumerEndpoint).build();
  }

  @Bean
  public PersistenceService persistenceService(PersonopplysningService personopplysningService, FarskapsportalEgenskaper farskapsportalEgenskaper,
      FarskapserklaeringDao farskapserklaeringDao, Mapper mapper, BarnDao barnDao, ForelderDao forelderDao,
      StatusKontrollereFarDao kontrollereFarDao, MeldingsloggDao meldingsloggDao) {
    return new PersistenceService(personopplysningService, farskapserklaeringDao, barnDao, forelderDao, kontrollereFarDao,
        meldingsloggDao, mapper);
  }

  @Bean
  public PersonopplysningService personopplysningService(PdlApiConsumer pdlApiConsumer) {
    return PersonopplysningService.builder().pdlApiConsumer(pdlApiConsumer).build();
  }

  @Bean
  public FarskapsportalService farskapsportalService(FarskapsportalEgenskaper farskapsportalEgenskaper, DifiESignaturConsumer difiESignaturConsumer,
      PdfGeneratorConsumer pdfGeneratorConsumer, PersistenceService persistenceService, PersonopplysningService personopplysningService,
      SkattConsumer skattConsumer,
      Mapper mapper) {

    return FarskapsportalService.builder().farskapsportalEgenskaper(farskapsportalEgenskaper).difiESignaturConsumer(difiESignaturConsumer)
        .pdfGeneratorConsumer(pdfGeneratorConsumer).persistenceService(persistenceService).personopplysningService(personopplysningService)
        .skattConsumer(skattConsumer)
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
  public class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource) throws InterruptedException {
      Thread.sleep(30000);
      Flyway.configure().dataSource(dataSource).load().migrate();
    }
  }
}
