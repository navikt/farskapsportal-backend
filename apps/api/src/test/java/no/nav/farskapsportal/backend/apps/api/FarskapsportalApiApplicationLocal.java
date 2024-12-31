package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.*;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.StorageOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceEnvironment;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.core.internal.security.ProvidesCertificateResourcePaths;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.stub.DifiESignaturStub;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.EncryptionProvider;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.*;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootApplication(
    exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = ASSIGNABLE_TYPE,
          value = {FarskapsportalApiApplication.class})
    })
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {
      "aapen-brukernotifikasjon-nyBeskjed-v1",
      "aapen-brukernotifikasjon-done-v1",
      "aapen-brukernotifikasjon-nyOppgave-v1"
    })
@EnableSecurityConfiguration
@EnableJwtTokenValidation(
    ignore = {
      "org.springdoc.webmvc.ui.SwaggerConfigResource",
      "org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController",
      "org.springdoc.webmvc.api.OpenApiWebMvcResource"
    })
@Slf4j
@EntityScan("no.nav.farskapsportal.backend.libs.entity")
public class FarskapsportalApiApplicationLocal {

  public static final String PADES = "/pades";
  public static final String XADES = "/xades";
  private static final String NAV_ORGNR = "123456789";

  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LOCAL : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplicationLocal.class);
    app.setAdditionalProfiles(profile, "local-nais");
    app.run(args);
  }

  @Bean
  @Primary
  @Profile({
    PROFILE_TEST,
    PROFILE_LOCAL,
    PROFILE_LOCAL_POSTGRES,
    PROFILE_REMOTE_POSTGRES,
    PROFILE_LOCAL_NAIS
  })
  public ClientConfiguration clientConfiguration(
      KeyStoreConfig keyStoreConfig, @Value("${url.esignering}") String esigneringUrl)
      throws URISyntaxException {

    var serviceEnvironmentLocal =
        new ServiceEnvironment(
            "Lokal test",
            new URI(esigneringUrl + "/esignering"),
            Certificates.TEST.certificatePaths);
    return ClientConfiguration.builder(keyStoreConfig)
        .serviceEnvironment(serviceEnvironmentLocal)
        .serviceEnvironment(serviceEnvironmentLocal)
        .defaultSender(new Sender(NAV_ORGNR))
        .build();
  }

  enum Certificates implements ProvidesCertificateResourcePaths {
    TEST(
        new String[] {
          "test/Buypass_Class_3_Test4_CA_3.cer",
          "test/Buypass_Class_3_Test4_Root_CA.cer",
          "test/BPCl3CaG2HTBS.cer",
          "test/BPCl3CaG2STBS.cer",
          "test/BPCl3RootCaG2HT.cer",
          "test/BPCl3RootCaG2ST.cer",
          "test/commfides_test_ca.cer",
          "test/commfides_test_root_ca.cer",
          "test/digipost_test_root_ca.cert.pem"
        });

    final List<String> certificatePaths;

    Certificates(String... certificatePaths) {
      this.certificatePaths =
          Stream.of(certificatePaths)
              .map("classpath:/certificates/"::concat)
              .collect(Collectors.toList());
    }

    public List<String> certificatePaths() {
      return this.certificatePaths();
    }
  }

  @Configuration
  @Profile({PROFILE_LOCAL_POSTGRES})
  static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource) {

      Flyway.configure().baselineOnMigrate(true).dataSource(dataSource).load().migrate();
    }
  }

  @Configuration
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  @EnableMockOAuth2Server
  @AutoConfigureWireMock(port = 0)
  class MockOauthServerLocalConfig {

    public MockOauthServerLocalConfig(@Autowired DifiESignaturStub difiESignaturStub) {
      difiESignaturStub.runGetSignedDocument(PADES);
      difiESignaturStub.runGetXades(XADES);
    }
  }

  @Configuration
  @Testcontainers
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  class LocalConfig {

    @Container
    static final GenericContainer<?> fakeGcs =
        new GenericContainer<>("fsouza/fake-gcs-server")
            .withExposedPorts(4443)
            .withCreateContainerCmdModifier(
                cmd -> cmd.withEntrypoint("/bin/fake-gcs-server", "-scheme", "http"));

    @Value("${APPNAVN}")
    private String appnavn;

    private static void updateExternalUrlWithContainerUrl(String fakeGcsExternalUrl)
        throws Exception {

      String modifyExternalUrlRequestUri = fakeGcsExternalUrl + "/_internal/config";
      String updateExternalUrlJson = "{" + "\"externalUrl\": \"" + fakeGcsExternalUrl + "\"" + "}";

      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(modifyExternalUrlRequestUri))
              .header("Content-Type", "application/json")
              .PUT(HttpRequest.BodyPublishers.ofString(updateExternalUrlJson))
              .build();
      HttpResponse<Void> response =
          HttpClient.newBuilder().build().send(req, HttpResponse.BodyHandlers.discarding());

      if (response.statusCode() != 200) {
        throw new RuntimeException(
            "error updating fake-gcs-server with external url, response status code "
                + response.statusCode()
                + " != 200");
      }
    }

    @Bean
    @Primary
    public EncryptionProvider encryptionProvider() {
      return new FakeEncryption();
    }

    @Bean
    @Primary
    public GcpStorageManager storageManager(EncryptionProvider encryptionProvider)
        throws Exception {

      fakeGcs.start();

      String fakeGcsExternalUrl =
          "http://" + fakeGcs.getHost() + ":" + fakeGcs.getFirstMappedPort();

      updateExternalUrlWithContainerUrl(fakeGcsExternalUrl);

      var storage =
          StorageOptions.newBuilder()
              .setHost(fakeGcsExternalUrl)
              .setProjectId("test-project")
              .setCredentials(NoCredentials.getInstance())
              .build()
              .getService();

      storage.create(BucketInfo.newBuilder(appnavn + "-dev-pades").build());
      storage.create(BucketInfo.newBuilder(appnavn + "-dev-xades").build());

      return new GcpStorageManager(encryptionProvider, storage, false);
    }
  }

  @Component
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  class FakeEncryption implements EncryptionProvider {

    @Override
    public int getKeyVersion() {
      return 0;
    }

    @Override
    public byte[] encrypt(byte[] fileContent, byte[] metadata) {
      return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] fileContent, byte[] metadata) {
      return new byte[0];
    }
  }
}
