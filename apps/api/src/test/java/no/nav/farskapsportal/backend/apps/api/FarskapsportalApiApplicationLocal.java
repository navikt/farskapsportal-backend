package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_REMOTE_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootApplication(
    exclude = {
      SecurityAutoConfiguration.class,
      ManagementWebSecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class,
      ServletWebSecurityAutoConfiguration.class,
    })
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = ASSIGNABLE_TYPE,
          value = {FarskapsportalApiApplication.class})
    })
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
      "listeners=EXTERNAL://localhost:0,CONTROLLER://localhost:0",
      "listener.security.protocol.map=EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT",
      "controller.listener.names=CONTROLLER",
      "inter.broker.listener.name=EXTERNAL"
    },
    topics = {
      "aapen-brukervarsel-v1",
    })
@EnableSecurityConfiguration
@EnableJwtTokenValidation(
    ignore = {
      "org.springdoc.webmvc.ui.SwaggerConfigResource",
      "org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController",
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

    // @EnableWireMock (org.wiremock.spring) kobler seg på JUnits Spring TestContext-rammeverk
    // (ContextCustomizerFactory) og starter derfor ALDRI wiremock-serveren når appen kjøres
    // som en vanlig SpringApplication via main() (kun når konteksten lastes via en
    // @SpringBootTest).
    // Vi reserverer derfor selv en ledig port her, tidlig, slik at ${wiremock.server.port} er
    // korrekt satt i Environment før andre beans (PdlApiConsumer, OppgaveApiConsumer, m.fl.)
    // leser den via WIREMOCK_URL. Samme mønster som mock-oauth2-server-biblioteket bruker selv.
    ensureWiremockPortIsReserved();

    SpringApplication app = new SpringApplication(FarskapsportalApiApplicationLocal.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

  private static void ensureWiremockPortIsReserved() {
    if (System.getProperty("wiremock.server.port") != null) {
      return;
    }
    try (ServerSocket ledigPort = new ServerSocket(0)) {
      System.setProperty("wiremock.server.port", String.valueOf(ledigPort.getLocalPort()));
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Kunne ikke reservere ledig port for lokal WireMock-server", e);
    }
  }

  @Bean
  @Primary
  @Profile({PROFILE_TEST, PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
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

      // V11_3_0 bruker placeholderen ${user_asynkron}.
      // Siden vi her kjører Flyway manuelt, må vi sette den selv. Lokalt finnes det ingen egen
      // asynkron-bruker,
      // så vi peker den til den lokale databasebrukeren.
      Flyway.configure()
          .baselineOnMigrate(true)
          .dataSource(dataSource)
          .placeholders(java.util.Map.of("user_asynkron", "cloudsqliamuser"))
          .load()
          .migrate();
    }
  }

  @Configuration
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  @EnableMockOAuth2Server
  class MockOauthServerLocalConfig {

    @Bean(destroyMethod = "stop")
    public static WireMockServer wireMockServer(
        @Value("${wiremock.server.port}") int wiremockPort) {
      WireMockServer wireMockServer =
          new WireMockServer(WireMockConfiguration.options().port(wiremockPort));
      wireMockServer.start();
      WireMock.configureFor("localhost", wiremockPort);
      return wireMockServer;
    }

    public MockOauthServerLocalConfig(
        @Autowired DifiESignaturStub difiESignaturStub, @Autowired WireMockServer wireMockServer) {
      difiESignaturStub.runGetSignedDocument(PADES);
      difiESignaturStub.runGetXades(XADES);
    }
  }

  @Configuration
  @Testcontainers
  @Profile("!live")
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
