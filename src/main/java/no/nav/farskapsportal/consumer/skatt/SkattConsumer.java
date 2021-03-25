package no.nav.farskapsportal.consumer.skatt;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
import no.nav.farskapsportal.consumer.skatt.api.Informasjonsmottak;
import no.nav.farskapsportal.consumer.skatt.api.Innsender;
import no.nav.farskapsportal.consumer.skatt.api.InnsendertypeForRegistreringAvFarskap;
import no.nav.farskapsportal.consumer.skatt.api.KanalForRegistreringAvFarskap;
import no.nav.farskapsportal.consumer.skatt.api.Mor;
import no.nav.farskapsportal.consumer.skatt.api.SaksbehandlersVurdering;
import no.nav.farskapsportal.consumer.skatt.api.Tekst;
import no.nav.farskapsportal.consumer.skatt.api.Vedlegg;
import no.nav.farskapsportal.exception.SkattConsumerException;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@AllArgsConstructor
@Slf4j
public class SkattConsumer {

  private final RestTemplate restTemplate;

  private final ConsumerEndpoint consumerEndpoint;

  public void registrereFarskap(Farskapserklaering farskapserklaering) {

    var xml = byggeMeldingTilSkatt(farskapserklaering);
    MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);//Main request's headers

    HttpHeaders requestHeadersAttachment = new HttpHeaders();
    requestHeadersAttachment.setContentType(MediaType.IMAGE_PNG);// extract mediatype from file extension
    HttpEntity<ByteArrayResource> attachmentPart;
    ByteArrayResource fileAsResource = new ByteArrayResource(farskapserklaering.getDokument().getInnhold()) {
      @Override
      public String getFilename() {
        return farskapserklaering.getDokument().getDokumentnavn();
      }
    };

    attachmentPart = new HttpEntity<>(fileAsResource, requestHeadersAttachment);
    multipartRequest.set("vedlegg", attachmentPart);

    HttpHeaders requestHeadersJSON = new HttpHeaders();
    requestHeadersJSON.setContentType(MediaType.APPLICATION_XML);

    HttpEntity<String> requestEntityXml = new HttpEntity<>(xml, requestHeadersJSON);
    multipartRequest.set("xml", requestEntityXml);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartRequest, requestHeaders);//final request

    try {
      restTemplate.exchange(
          consumerEndpoint.retrieveEndpoint(
              SkattEndpointName.MOTTA_FARSKAPSERKLAERING),
          HttpMethod.POST,
          requestEntity, Void.class);
    } catch (Exception e) {
      throw new SkattConsumerException(Feilkode.SKATT_OVERFOERING_FEILET);
    }
  }

  private String byggeMeldingTilSkatt(Farskapserklaering farskapserklaering) {

    try {
      var forespoerselOmRegistreringAvFarskap = tilForespoerselOmRegistreringAvFarskap(farskapserklaering);

      var xmlString = new StringWriter();
      Marshaller marshaller = JAXBContext.newInstance(ForespoerselOmRegistreringAvFarskap.class).createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(forespoerselOmRegistreringAvFarskap, xmlString);

      return xmlString.toString();
    } catch (JAXBException jaxbe) {
      throw new SkattConsumerException(Feilkode.SKATT_MELDINGSFORMAT, jaxbe);
    }
  }

  private ForespoerselOmRegistreringAvFarskap tilForespoerselOmRegistreringAvFarskap(Farskapserklaering farskapserklaering) {

    return ForespoerselOmRegistreringAvFarskap.builder()
        .far(Far.builder()
            .datoForErklaeringen(henteDatoForErklaeringen(farskapserklaering))
            .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaering.getFar().getFoedselsnummer()))
            .harSignert(tilBoolsk(farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null))
            .build())
        .mor(Mor.builder()
            .datoForErklaeringen(henteDatoForErklaeringen(farskapserklaering))
            .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaering.getMor().getFoedselsnummer()))
            .harSignert(tilBoolsk(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null))
            .build())
        .barnet(Barn.builder()
            .erFoedt(tilBoolsk(farskapserklaering.getBarn().getFoedselsnummer() != null))
            .termindato(tilDato(farskapserklaering.getBarn().getTermindato()))
            .foedselsEllerDNummer(
                farskapserklaering.getBarn().getFoedselsnummer() != null ? tilFoedsedslsnummer(farskapserklaering.getBarn().getFoedselsnummer())
                    : null).build())
        .registreringsdato(tilDato(LocalDate.now()))
        .innsender(new Innsender(new InnsendertypeForRegistreringAvFarskap("nav")))
        .mottak(Informasjonsmottak.builder()
            .informasjonskanal(new KanalForRegistreringAvFarskap("elektroniskMelding"))
            .mottakstidspunktFraOpprinneligkanal(tilDato(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()))
            .build())
        .saksbehandlersVurdering(SaksbehandlersVurdering.builder().skjemaErAttestert(tilBoolsk(true))
            .vedlagtFarskapsskjemaErOriginalt(tilBoolsk(true)).build())
        .vedlegg(new Vedlegg(new Tekst(farskapserklaering.getDokument().getDokumentnavn())))
        .foreldreBorSammen(new Boolsk(true))
        .build();
  }

  private Dato henteDatoForErklaeringen(Farskapserklaering farskapserklaering) {
    if (farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null) {
      throw new SkattConsumerException(Feilkode.FARSKAPSERKLAERING_MANGLER_SIGNATUR);
    }
    var signeringstidspunktFar = farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt();
    return tilDato(signeringstidspunktFar);
  }

  private Dato tilDato(LocalDateTime localDateTime) {
    return tilDato(LocalDate.from(localDateTime));
  }

  private Dato tilDato(LocalDate localDate) {
    try {
      XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDate.from(localDate).toString());
      return new Dato(xmlGregorianCalendar);
    } catch (DatatypeConfigurationException dce) {
      throw new SkattConsumerException(Feilkode.SKATT_MELDINGSFORMAT, dce);
    }
  }

  private Foedselsnummer tilFoedsedslsnummer(String foedselsnummer) {
    return new Foedselsnummer(new Tekst(foedselsnummer));
  }

  private Boolsk tilBoolsk(boolean sjekk) {
    return new Boolsk(sjekk);
  }

}
