package no.nav.farskapsportal.config;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.consumer.brukernotifikasjon.Beskjedprodusent;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.brukernotifikasjon.Ferdigprodusent;
import no.nav.farskapsportal.consumer.brukernotifikasjon.Oppgaveprodusent;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrukernotifikasjonConfig {

  public static final String GRUPPERINGSID_FARSKAP = "1";
  private static final String TOPIC_BESKJED = "aapen-brukernotifikasjon-nyBeskjed-v1-testing";
  private static final String TOPIC_FERDIG = "aapen-brukernotifikasjon-nyDone-v1-testing";
  private static final String TOPIC_OPPGAVE = "aapen-brukernotifikasjon-nyOppgave-v1-testing";

  @Value("${farskapsportal-api.systembruker}")
  private String systembruker;

  @Bean
  Koestyrer koestyrer(@Autowired  KafkaProducer<Nokkel, SpecificRecord> kafkaProducer) {
    return new Koestyrer(new KafkaProducer<Nokkel, SpecificRecord>());
  }

  @Bean
  Properties brukernotifikasjonKoeEgenskaper(@Value("consumer.brukernotifikasjon.kafka-bootstrap-servers") String kafkaBootstrapServers,
      @Value("${consumer.brukernotifikasjon.kafka-schema-registry-url}") String kafkaSchemaRegistryUrl) {
    var brukernotifikasjonKoeEgenskaper = new Properties();
    brukernotifikasjonKoeEgenskaper.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
    brukernotifikasjonKoeEgenskaper.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
    brukernotifikasjonKoeEgenskaper.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
    brukernotifikasjonKoeEgenskaper.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaSchemaRegistryUrl);
    return brukernotifikasjonKoeEgenskaper;
  }

  @Bean
  BrukernotifikasjonConsumer brukernotifikasjonConsumer(Beskjedprodusent beskjedprodusent, Ferdigprodusent ferdigprodusent,
      Oppgaveprodusent oppgaveprodusent) {
    return new BrukernotifikasjonConsumer(beskjedprodusent, ferdigprodusent, oppgaveprodusent);
  }

  @Bean
  Beskjedprodusent beskjedprodusent(@Value("consumer.brukernotifikasjon.synlighet.beskjed-antall-maaneder") int beskjedSynligIAntallMaaneder,
      @Value("${consumer.brukernotifikasjon.sikkerhetsnivaa.beskjed}") int sikkerhetsNivaaBeskjed, Properties brukernotifikasjonKoeEgenskaper) {
    var brukernotifikasjonKafkaProducer = new KafkaProducer<Nokkel, Beskjed>(brukernotifikasjonKoeEgenskaper);
    return new Beskjedprodusent(TOPIC_BESKJED, sikkerhetsNivaaBeskjed, beskjedSynligIAntallMaaneder, brukernotifikasjonKafkaProducer);
  }

  @Bean
  Oppgaveprodusent oppgaveprodusent(
      @Value("consumer.brukernotifikasjon.synlighet.oppgave-antall-dager") int oppgaveSynligIAntallDager,
      @Value("${consumer.brukernotifikasjon.sikkerhetsnivaa.oppgave}") int sikkerhetsNivaaOppgave,
      @Value("url.farskapsportal") String urlFarskapsportal,
      Properties brukernotifikasjonKoeEgenskaper) throws MalformedURLException {
    var brukernotifikasjonKafkaProducer = new KafkaProducer<Nokkel, Oppgave>(brukernotifikasjonKoeEgenskaper);

    return new Oppgaveprodusent(TOPIC_OPPGAVE, oppgaveSynligIAntallDager, sikkerhetsNivaaOppgave, systembruker, oppretteUrl(urlFarskapsportal), brukernotifikasjonKafkaProducer);
  }

  @Bean
  Ferdigprodusent ferdigprodusent(Properties brukernotifikasjonKoeEgenskaper) {
    var brukernotifikasjonKafkaProducer = new KafkaProducer<Nokkel, Done>(brukernotifikasjonKoeEgenskaper);
    return new Ferdigprodusent(TOPIC_FERDIG, systembruker, brukernotifikasjonKafkaProducer);
  }

  private URL oppretteUrl(String url) throws MalformedURLException {
    return new URL(url);
  }
}
