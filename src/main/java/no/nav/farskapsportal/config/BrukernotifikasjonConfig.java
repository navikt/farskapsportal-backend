package no.nav.farskapsportal.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.Beskjedprodusent;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.brukernotifikasjon.Ferdigprodusent;
import no.nav.farskapsportal.consumer.brukernotifikasjon.Oppgaveprodusent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class BrukernotifikasjonConfig {

  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  @Value("${kafka.bootstrap-servers}")
  private String bootstrapAddress;

  public BrukernotifikasjonConfig(@Autowired FarskapsportalEgenskaper farskapsportalEgenskaper) {
    this.farskapsportalEgenskaper = farskapsportalEgenskaper;
  }

  private Map<String, Object> getConfigProps() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return configProps;
  }

  @Bean("beskjed")
  public KafkaTemplate<Nokkel, Beskjed> kafkaTemplateBeskjed() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getConfigProps()));
  }

  @Bean("ferdig")
  public KafkaTemplate<Nokkel, Done> kafkaTemplateFerdig() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getConfigProps()));
  }

  @Bean("oppgave")
  public KafkaTemplate<Nokkel, Oppgave> kafkaTemplateOppgave() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getConfigProps()));
  }

  @Bean
  BrukernotifikasjonConsumer brukernotifikasjonConsumer(Beskjedprodusent beskjedprodusent, Ferdigprodusent ferdigprodusent,
      Oppgaveprodusent oppgaveprodusent) throws MalformedURLException {
    return new BrukernotifikasjonConsumer(toUrl(farskapsportalEgenskaper.getUrl()), beskjedprodusent, ferdigprodusent, oppgaveprodusent);
  }

  @Bean
  Beskjedprodusent beskjedprodusent(@Qualifier("beskjed") KafkaTemplate<Nokkel, Beskjed> kafkaTemplate) {
    return new Beskjedprodusent(farskapsportalEgenskaper, kafkaTemplate);
  }

  @Bean
  Oppgaveprodusent oppgaveprodusent(
      @Qualifier("oppgave") KafkaTemplate<Nokkel, Oppgave> kafkaTemplate) throws MalformedURLException {
    return new Oppgaveprodusent(farskapsportalEgenskaper, toUrl(farskapsportalEgenskaper.getUrl()),  kafkaTemplate);
  }

  @Bean
  Ferdigprodusent ferdigprodusent(@Qualifier("ferdig") KafkaTemplate<Nokkel, Done> kafkaTemplate) {
    return new Ferdigprodusent(farskapsportalEgenskaper, kafkaTemplate);
  }

  public URL toUrl(String url) throws MalformedURLException {
    return new URL(url);
  }
}
