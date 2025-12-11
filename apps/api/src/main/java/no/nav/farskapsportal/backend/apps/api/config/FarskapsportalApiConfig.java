package no.nav.farskapsportal.backend.apps.api.config;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.StorageOptions;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import no.nav.bidrag.commons.security.service.OidcTokenManager;
import no.nav.bidrag.commons.security.utils.TokenUtils;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalApiEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumerEndpoint;
import no.nav.farskapsportal.backend.apps.api.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattEndpoint;
import no.nav.farskapsportal.backend.apps.api.model.Skriftspraak;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.apps.api.service.Mapper;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.*;
import no.nav.farskapsportal.backend.libs.felles.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.backend.libs.felles.secretmanager.FarskapKeystoreCredentials;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.flywaydb.core.Flyway;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Slf4j
@Configuration
@EnableSecurityConfiguration
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP)
@OpenAPIDefinition(
    info = @Info(title = "farskapsportal-api", version = "v1"),
    security = @SecurityRequirement(name = "bearer-key"))
@ComponentScan("no.nav.farskapsportal.backend")
public class FarskapsportalApiConfig {

  @Autowired private OidcTokenManager oidcTokenManager;

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

  @Bean
  public PdlApiConsumer pdlApiConsumer(
      @Qualifier("pdl-api") RestTemplate restTemplate,
      @Value("${url.pdl-api.base-url}") String baseUrl,
      @Value("${url.pdl-api.graphql}") String pdlApiEndpoint,
      ConsumerEndpoint consumerEndpoint) {
    consumerEndpoint.addEndpoint(PdlApiConsumerEndpointName.PDL_API_GRAPHQL, pdlApiEndpoint);
    restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl));
    log.info("Oppretter PdlApiConsumer med url {}", baseUrl);
    return PdlApiConsumer.builder()
        .restTemplate(restTemplate)
        .consumerEndpoint(consumerEndpoint)
        .build();
  }

  @Bean
  public PersonopplysningService personopplysningService(
      ModelMapper modelMapper,
      PdlApiConsumer pdlApiConsumer,
      FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper) {
    return PersonopplysningService.builder()
        .modelMapper(modelMapper)
        .pdlApiConsumer(pdlApiConsumer)
        .farskapsportalFellesEgenskaper(farskapsportalFellesEgenskaper)
        .build();
  }

  @Bean
  public FarskapsportalService farskapsportalService(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      BucketConsumer bucketConsumer,
      FarskapsportalApiEgenskaper farskapsportalApiEgenskaper,
      DifiESignaturConsumer difiESignaturConsumer,
      PdfGeneratorConsumer pdfGeneratorConsumer,
      PersistenceService persistenceService,
      PersonopplysningService personopplysningService,
      Mapper mapper) {

    return FarskapsportalService.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .bucketConsumer(bucketConsumer)
        .farskapsportalApiEgenskaper(farskapsportalApiEgenskaper)
        .difiESignaturConsumer(difiESignaturConsumer)
        .pdfGeneratorConsumer(pdfGeneratorConsumer)
        .persistenceService(persistenceService)
        .personopplysningService(personopplysningService)
        .mapper(mapper)
        .build();
  }

  @Bean
  public OppgaveApiConsumer oppgaveApiConsumer(
      @Qualifier("oppgave") RestTemplate restTemplate,
      @Value("${url.oppgave.opprette}") String oppretteOppgaveEndpoint,
      ConsumerEndpoint consumerEndpoint) {
    consumerEndpoint.addEndpoint(
        OppgaveApiConsumerEndpoint.OPPRETTE_OPPGAVE_ENDPOINT_NAME, oppretteOppgaveEndpoint);
    return new OppgaveApiConsumer(restTemplate, consumerEndpoint);
  }

  @Bean
  @Qualifier("skatt")
  @Profile({PROFILE_LIVE, PROFILE_INTEGRATION_TEST})
  public KeyStoreConfig keyStoreConfigSkatt(
      @Value("${virksomhetssertifikat.prosjektid}") String virksomhetssertifikatProsjektid,
      @Value("${virksomhetssertifikat.hemmelighetnavn}")
          String virksomhetssertifikatHemmelighetNavn,
      @Value("${virksomhetssertifikat.hemmelighetversjon}")
          String virksomhetssertifikatHemmelighetVersjon,
      @Value("${virksomhetssertifikat.passord.prosjektid}")
          String virksomhetssertifikatPassordProsjektid,
      @Value("${virksomhetssertifikat.passord.hemmelighetnavn}")
          String virksomhetssertifikatPassordHemmelighetNavn,
      @Value("${virksomhetssertifikat.passord.hemmelighetversjon}")
          String virksomhetssertifikatPassordHemmelighetVersjon,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion)
      throws IOException {

    var sertifikatpassord =
        accessSecretVersion
            .accessSecretVersion(
                virksomhetssertifikatPassordProsjektid,
                virksomhetssertifikatPassordHemmelighetNavn,
                virksomhetssertifikatPassordHemmelighetVersjon)
            .getData()
            .toStringUtf8();

    var objectMapper = new ObjectMapper();
    var farskapKeystoreCredentials =
        objectMapper.readValue(sertifikatpassord, FarskapKeystoreCredentials.class);

    log.info("lengde sertifikatpassord {}", farskapKeystoreCredentials.getPassword().length());

    var secretPayload =
        accessSecretVersion.accessSecretVersion(
            virksomhetssertifikatProsjektid,
            virksomhetssertifikatHemmelighetNavn,
            virksomhetssertifikatHemmelighetVersjon);

    log.info("lengde sertifikat: {}", secretPayload.getData().size());
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig.fromJavaKeyStore(
        inputStream,
        farskapKeystoreCredentials.getAlias(),
        farskapKeystoreCredentials.getPassword(),
        farskapKeystoreCredentials.getPassword());
  }

  @Bean
  public SkattConsumer skattConsumer(
      CloseableHttpClient httpClient,
      @Value("${SKATT_URL}") String skattBaseUrl,
      @Value("${url.skatt.registrering-av-farskap}") String endpoint,
      ConsumerEndpoint consumerEndpoint,
      BucketConsumer bucketConsumer) {
    consumerEndpoint.addEndpoint(SkattEndpoint.MOTTA_FARSKAPSERKLAERING, skattBaseUrl + endpoint);
    return new SkattConsumer(httpClient, consumerEndpoint, bucketConsumer);
  }

  @Bean
  @Profile(PROFILE_LIVE)
  public EncryptionProvider encryptionProvider(@Value("${GCP_KMS_KEY_PATH}") String gcpKmsKeyPath)
      throws GeneralSecurityException, IOException {
    return new GcpCloudKms(gcpKmsKeyPath);
  }

  @Bean
  @Profile(PROFILE_LIVE)
  public GcpStorageManager storageManager(
      EncryptionProvider encryptionProvider,
      @Value("${farskapsportal.egenskaper.kryptering-paa}") boolean krypteringPaa)
      throws GeneralSecurityException, IOException {
    return new GcpStorageManager(
        encryptionProvider, StorageOptions.getDefaultInstance().getService(), krypteringPaa);
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
  public OidcTokenPersonalIdExtractor oidcTokenPersonalIdExtractor() {
    return TokenUtils::hentBruker;
  }

  @FunctionalInterface
  public interface OidcTokenPersonalIdExtractor {

    String hentPaaloggetPerson();
  }

  @Configuration
  @Profile(PROFILE_LIVE)
  public static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(
        @Qualifier("dataSource") DataSource dataSource,
        @Value("${spring.flyway.placeholders.user}") String dbUserAsynkron)
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

  @Configuration
  @Profile({PROFILE_LIVE, PROFILE_LOCAL_POSTGRES})
  @EnableScheduling
  @EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
  public class SchedulerConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
      return new JdbcTemplateLockProvider(dataSource);
    }
  }
}
