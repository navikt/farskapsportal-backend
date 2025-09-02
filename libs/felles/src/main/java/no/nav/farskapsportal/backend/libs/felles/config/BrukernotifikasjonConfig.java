package no.nav.farskapsportal.backend.libs.felles.config;

import static io.netty.util.NetUtil.getHostname;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.Beskjedprodusent;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.Ferdigprodusent;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.Oppgaveprodusent;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class BrukernotifikasjonConfig {

  public static final String NAMESPACE_FARSKAPSPORTAL = "farskapsportal";

  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Value("${KAFKA_SCHEMA_REGISTRY_USER}:${KAFKA_SCHEMA_REGISTRY_PASSWORD}")
  private String aivenSchemaRegistryCredentials;

  @Value("${KAFKA_BROKERS}")
  private String bootstrapAddress;

  @Value("${KAFKA_SCHEMA_REGISTRY}")
  private String kafkaSchemaRegistryUrlConfig;

  @Value("${KAFKA_KEYSTORE_PATH}")
  private String keyStorePath;

  @Value("${KAFKA_TRUSTSTORE_PATH}")
  private String trustStorePath;

  @Value("${KAFKA_CREDSTORE_PASSWORD}")
  private String trustStorePwd;

  @Value("${KAFKA_CREDSTORE_PASSWORD}")
  private String sslKeyPassword;

  public BrukernotifikasjonConfig(
      @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper) {
    this.farskapsportalFellesEgenskaper = farskapsportalFellesEgenskaper;
  }

  private Map<String, Object> getKafkaConfigProps() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 1);
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(
        ProducerConfig.CLIENT_ID_CONFIG,
        NAMESPACE_FARSKAPSPORTAL + getHostname(new InetSocketAddress(0)));
    configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    configProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
    configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
    configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePwd);
    configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStorePath);
    configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, trustStorePwd);
    configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
    configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
    configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
    configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
    configProps.put("reconnect.backoff.ms", 100);
    return configProps;
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplateBrukernotifikasjon() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean
  BrukernotifikasjonConsumer brukernotifikasjonConsumer(
      Beskjedprodusent beskjedprodusent,
      Ferdigprodusent ferdigprodusent,
      Oppgaveprodusent oppgaveprodusent,
      FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper)
      throws MalformedURLException {
    return new BrukernotifikasjonConsumer(
        beskjedprodusent, ferdigprodusent, oppgaveprodusent, farskapsportalFellesEgenskaper);
  }

  @Bean
  Beskjedprodusent beskjedprodusent(KafkaTemplate<String, String> kafkaTemplate)
      throws MalformedURLException {
    return new Beskjedprodusent(
        kafkaTemplate,
        toUrl(farskapsportalFellesEgenskaper.getUrl()),
        toUrl(farskapsportalFellesEgenskaper.getUrl() + "/oversikt"),
        farskapsportalFellesEgenskaper);
  }

  @Bean
  Oppgaveprodusent oppgaveprodusent(
      KafkaTemplate<String, String> kafkaTemplate, PersistenceService persistenceService)
      throws MalformedURLException {
    return new Oppgaveprodusent(
        kafkaTemplate,
        persistenceService,
        toUrl(farskapsportalFellesEgenskaper.getUrl()),
        farskapsportalFellesEgenskaper);
  }

  @Bean
  Ferdigprodusent ferdigprodusent(
      KafkaTemplate<String, String> kafkaTemplate,
      PersistenceService persistenceService,
      OppgavebestillingDao oppgavebestillingDao) {
    return new Ferdigprodusent(
        kafkaTemplate, persistenceService, oppgavebestillingDao, farskapsportalFellesEgenskaper);
  }

  private URL toUrl(String url) throws MalformedURLException {
    return new URL(url);
  }
}
