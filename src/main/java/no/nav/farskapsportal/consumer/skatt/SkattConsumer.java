package no.nav.farskapsportal.consumer.skatt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.skatt.api.Barn;
import no.nav.farskapsportal.consumer.skatt.api.Boolsk;
import no.nav.farskapsportal.consumer.skatt.api.Dato;
import no.nav.farskapsportal.consumer.skatt.api.Far;
import no.nav.farskapsportal.consumer.skatt.api.Foedselsnummer;
import no.nav.farskapsportal.consumer.skatt.api.ForespoerselOmRegistreringAvFarskap;
import no.nav.farskapsportal.consumer.skatt.api.Innsender;
import no.nav.farskapsportal.consumer.skatt.api.Mor;
import no.nav.farskapsportal.consumer.skatt.api.Tekst;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.exception.DatamappingException;
import org.springframework.web.client.RestTemplate;

@AllArgsConstructor
@Slf4j
public class SkattConsumer {

  private final RestTemplate restTemplate;

  private final ConsumerEndpoint consumerEndpoint;

  public void registrereFarskap(FarskapserklaeringDto farskapserklaeringDto) {

    var forespoerselOmRegistreringAvFarskap = tilForespoerselOmRegistreringAvFarskap(farskapserklaeringDto);

  }

  private ForespoerselOmRegistreringAvFarskap tilForespoerselOmRegistreringAvFarskap(FarskapserklaeringDto farskapserklaeringDto) {

    return ForespoerselOmRegistreringAvFarskap.builder()
        .far(Far.builder()
            .datoForErklaeringen(henteDatoForErklaeringen(farskapserklaeringDto))
            .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaeringDto.getFar().getFoedselsnummer()))
            .harSignert(tilBoolsk(farskapserklaeringDto.getDokument().getSignertAvFar() != null))
            .build())
        .mor(Mor.builder()
            .datoForErklaeringen(henteDatoForErklaeringen(farskapserklaeringDto))
            .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaeringDto.getMor().getFoedselsnummer()))
            .harSignert(tilBoolsk(farskapserklaeringDto.getDokument().getSignertAvMor() != null))
            .build())
        .barnet(Barn.builder()
            .erFoedt(tilBoolsk(farskapserklaeringDto.getBarn().getFoedselsnummer() != null))
            .termindato(tilDato(farskapserklaeringDto.getBarn().getTermindato()))
            .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaeringDto.getBarn().getFoedselsnummer()))
            .build())
        .registreringsdato(tilDato(LocalDate.now()))
        .innsender(Innsender.builder()
            .
            .build())
        .build();
  }

  private Dato henteDatoForErklaeringen(FarskapserklaeringDto farskapserklaeringDto) {
    var signeringstidspunktFar = farskapserklaeringDto.getDokument().getSignertAvFar();
    return tilDato(signeringstidspunktFar);
  }

  private Dato tilDato(LocalDateTime localDateTime) {
    return tilDato(LocalDate.from(localDateTime));
  }

  private Dato tilDato(LocalDate localDate)  {
    try {
      XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDate.from(localDate).toString());
      return new Dato(xmlGregorianCalendar);
    } catch (DatatypeConfigurationException dce) {
      throw new DatamappingException(Feilkode.SKATT_MELDINGSFORMAT, dce);
    }
  }

  private Foedselsnummer tilFoedsedslsnummer(String foedselsnummer) {
    return new Foedselsnummer(new Tekst(foedselsnummer));
  }

  private Boolsk tilBoolsk(boolean sjekk) {
    return new Boolsk(sjekk);
  }

}
