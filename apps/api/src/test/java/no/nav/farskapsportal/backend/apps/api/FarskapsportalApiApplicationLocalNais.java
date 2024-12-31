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
@EnableSecurityConfiguration
@EnableMockOAuth2Server
@EnableJwtTokenValidation(
    ignore = {
      "org.springdoc.webmvc.ui.SwaggerConfigResource",
      "org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController",
      "org.springdoc.webmvc.api.OpenApiWebMvcResource"
    })
@Slf4j
@EntityScan("no.nav.farskapsportal.backend.libs.entity")
public class FarskapsportalApiApplicationLocalNais {

  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LOCAL_NAIS : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplicationLocalNais.class);
    app.setAdditionalProfiles(profile, "local-nais-secrets");
    app.run(args);
  }

}
