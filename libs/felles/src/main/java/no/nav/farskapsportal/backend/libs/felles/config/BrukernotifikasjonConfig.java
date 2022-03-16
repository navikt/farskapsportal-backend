package no.nav.farskapsportal.backend.libs.felles.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.Beskjedprodusent;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.Ferdigprodusent;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.Oppgaveprodusent;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class BrukernotifikasjonConfig {

  public static final String NAMESPACE_FARSKAPSPORTAL = "farskapsportal";

  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapAddress;

  @Value("${spring.kafka.properties.schema.registry.url.config}")
  private String kafkaSchemaRegistryUrlConfig;

  @Value("${spring.kafka.properties.ssl.truststore.location}")
  private String trustStorePath;

  @Value("${spring.kafka.properties.ssl.truststore.password}")
  private String trustStorePwd;

  @Value("${spring.kafka.properties.sasl.jaas.config}")
  private String saslJaasConfig;

  @Value("${spring.kafka.properties.sasl.mechanism}")
  private String saslMechanism;

  @Value("${spring.kafka.properties.security.protocol}")
  private String securityProtocol;

  public BrukernotifikasjonConfig(@Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper) {
    this.farskapsportalFellesEgenskaper = farskapsportalFellesEgenskaper;
  }

  private Map<String, Object> getKafkaConfigProps() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    configProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaSchemaRegistryUrlConfig);
    configProps.put("schema.registry.ssl.keystore.location", trustStorePath);
    configProps.put("schema.registry.ssl.keystore.password", trustStorePwd);
    configProps.put("schema.registry.ssl.truststore.location", trustStorePath);
    configProps.put("schema.registry.ssl.truststore.password", trustStorePwd);
    configProps.put("ssl.truststore.location", trustStorePath);
    configProps.put("ssl.truststore.password", trustStorePwd);
    configProps.put("ssl.keystore.location", trustStorePath);
    configProps.put("ssl.keystore.password", trustStorePwd);
    configProps.put("security.protocol", securityProtocol);
    configProps.put("sasl.jaas.config", saslJaasConfig);
    configProps.put("sasl.mechanism", saslMechanism);
    configProps.put("reconnect.backoff.ms", 100);
    return configProps;
  }

  @Bean("beskjed")
  public KafkaTemplate<NokkelInput, BeskjedInput> kafkaTemplateBeskjed() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean("ferdig")
  public KafkaTemplate<NokkelInput, DoneInput> kafkaTemplateFerdig() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean("oppgave")
  public KafkaTemplate<NokkelInput, OppgaveInput> kafkaTemplateOppgave() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean
  BrukernotifikasjonConsumer brukernotifikasjonConsumer(Beskjedprodusent beskjedprodusent, Ferdigprodusent ferdigprodusent,
      Oppgaveprodusent oppgaveprodusent, FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper) throws MalformedURLException {
    return new BrukernotifikasjonConsumer(beskjedprodusent, ferdigprodusent, oppgaveprodusent, farskapsportalFellesEgenskaper);
  }

  @Bean
  Beskjedprodusent beskjedprodusent(@Qualifier("beskjed") KafkaTemplate<NokkelInput, BeskjedInput> kafkaTemplate) throws MalformedURLException {
    return new Beskjedprodusent(kafkaTemplate, toUrl(farskapsportalFellesEgenskaper.getUrl()),
        toUrl(farskapsportalFellesEgenskaper.getUrl() + "/oversikt"), farskapsportalFellesEgenskaper);
  }

  @Bean
  Oppgaveprodusent oppgaveprodusent(
      @Qualifier("oppgave") KafkaTemplate<NokkelInput, OppgaveInput> kafkaTemplate, PersistenceService persistenceService)
      throws MalformedURLException {
    return new Oppgaveprodusent(kafkaTemplate, persistenceService, toUrl(farskapsportalFellesEgenskaper.getUrl()), farskapsportalFellesEgenskaper);
  }

  @Bean
  Ferdigprodusent ferdigprodusent(@Qualifier("ferdig") KafkaTemplate<NokkelInput, DoneInput> kafkaTemplate, PersistenceService persistenceService,
      OppgavebestillingDao oppgavebestillingDao) {
    return new Ferdigprodusent(kafkaTemplate, persistenceService, oppgavebestillingDao, farskapsportalFellesEgenskaper);
  }

  private URL toUrl(String url) throws MalformedURLException {
    return new URL(url);
  }
}
