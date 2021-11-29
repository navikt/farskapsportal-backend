package no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Barn;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Boolsk;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Dato;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.DatoKlokkeslett;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Far;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Foedselsnummer;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.ForespoerselOmRegistreringAvFarskap;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Informasjonsmottak;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Innsender;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.InnsendertypeForRegistreringAvFarskap;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Innsending;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.KanalForRegistreringAvFarskap;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.MeldingOmRegistreringAvFarskap;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Mor;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.SaksbehandlersVurdering;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Tekst;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.Vedlegg;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.apache.commons.lang3.Validate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
public class SkattConsumer {

  private static String AVSENDER_KILDESYSTEM = "FARSKAPSPORTAL";

  private final RestTemplate restTemplate;

  private final ConsumerEndpoint consumerEndpoint;

  @Retryable(value = RestClientException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000000))
  public LocalDateTime registrereFarskap(Farskapserklaering farskapserklaering) {

    var meldingOmRegistreringAvFarskap = tilMeldingOmRegistreringAvFarskap(farskapserklaering);
    var xml = tilStreng(meldingOmRegistreringAvFarskap);

    MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();

    if (farskapserklaering.getDokument().getDokumentinnhold().getInnhold().length < 1) {
      throw new SkattConsumerException(Feilkode.DOKUMENT_MANGLER_INNOHLD);
    }

    if (farskapserklaering.getDokument().getSigneringsinformasjonMor().getXadesXml().length < 1) {
      throw new SkattConsumerException((Feilkode.XADES_MOR_UTEN_INNHOLD));
    }

    if (farskapserklaering.getDokument().getSigneringsinformasjonFar().getXadesXml().length < 1) {
      throw new SkattConsumerException((Feilkode.XADES_FAR_UTEN_INNHOLD));
    }

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);//Main request's headers

    // Melding - Metadata som beskriver forsendelsen
    HttpHeaders requestHeadersJSON = new HttpHeaders();
    requestHeadersJSON.setContentType(MediaType.APPLICATION_XML);

    HttpEntity<String> requestEntityXml = new HttpEntity<>(xml, requestHeadersJSON);
    multipartRequest.set("melding", requestEntityXml);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartRequest, requestHeaders);

    // Vedlegg - Ferdig signert farskapserklæring PAdES
    var padesDokument = oppretteVedlegg(MediaType.APPLICATION_PDF, farskapserklaering.getDokument().getDokumentinnhold().getInnhold(),
        farskapserklaering.getDokument().getNavn());
    multipartRequest.set("vedlegg", padesDokument);

    // vedlegg2 - XADES mor
    var xadesXmlMor = oppretteVedlegg(MediaType.APPLICATION_XML, farskapserklaering.getDokument().getSigneringsinformasjonMor().getXadesXml(),
        "xadesMor.xml");
    multipartRequest.set("melding2", xadesXmlMor);

    // vedlegg3 - XADES far
    var xadesXmlFar = oppretteVedlegg(MediaType.APPLICATION_XML, farskapserklaering.getDokument().getSigneringsinformasjonFar().getXadesXml(),
        "xadesFar.xml");
    multipartRequest.set("melding3", xadesXmlFar);

    try {
      var respons = restTemplate.exchange(
          consumerEndpoint.retrieveEndpoint(
              SkattEndpoint.MOTTA_FARSKAPSERKLAERING),
          HttpMethod.POST,
          requestEntity, Void.class);
      if (!respons.getStatusCode().equals(HttpStatus.ACCEPTED)) {
        log.error("Mottok ikke-godkjent Http-kode {} ved overføring til Skatt", respons.getStatusCodeValue());
        throw new SkattConsumerException(Feilkode.SKATT_OVERFOERING_FEILET);
      }
      return LocalDateTime.parse(meldingOmRegistreringAvFarskap.getInnsending().getAvsendersInnsendingstidspunkt().getDateTime());
    } catch (Exception e) {
      e.printStackTrace();
      throw new SkattConsumerException(Feilkode.SKATT_OVERFOERING_FEILET, e);
    }
  }

  private HttpEntity<ByteArrayResource> oppretteVedlegg(MediaType mediaType, byte[] data, String dokumentnavn) {
    HttpHeaders requestHeadersVedlegg = new HttpHeaders();
    requestHeadersVedlegg.setContentType(mediaType);// extract mediatype from file extension

    var fileAsResource = new ByteArrayResource(data) {
      @Override
      public String getFilename() {
        return dokumentnavn;
      }
    };

    return new HttpEntity<>(fileAsResource, requestHeadersVedlegg);
  }

  private String tilStreng(MeldingOmRegistreringAvFarskap meldingOmRegistreringAvFarskap) {
    try {
      var xmlString = new StringWriter();
      Marshaller marshaller = JAXBContext.newInstance(MeldingOmRegistreringAvFarskap.class).createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(meldingOmRegistreringAvFarskap, xmlString);
      return xmlString.toString();
    } catch (JAXBException jaxbe) {
      throw new SkattConsumerException(Feilkode.SKATT_MELDINGSFORMAT, jaxbe);
    }
  }

  private MeldingOmRegistreringAvFarskap tilMeldingOmRegistreringAvFarskap(Farskapserklaering farskapserklaering) {

    validereFarskapserklaeringKlarTilOversendelse(farskapserklaering);

    return MeldingOmRegistreringAvFarskap.builder()
        .innsending(Innsending.builder()
            .avsendersInnsendingstidspunkt(tilDatoKlokkeslett(LocalDateTime.now()))
            .kildesystem(new Tekst(AVSENDER_KILDESYSTEM))
            .avsendersMeldingsidentifikator(new Tekst(farskapserklaering.getMeldingsidSkatt()))
            .build())
        .forespoersel(ForespoerselOmRegistreringAvFarskap.builder()
            .far(Far.builder()
                .datoForErklaeringen(henteDatoForErklaeringen(farskapserklaering))
                .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaering.getFar().getFoedselsnummer()))
                .harSignert(tilBoolsk(farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null))
                .build())
            .mor(Mor.builder()
                .foedselsEllerDNummer(tilFoedsedslsnummer(farskapserklaering.getMor().getFoedselsnummer()))
                .harSignert(tilBoolsk(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null))
                .build())
            .barnet(Barn.builder()
                .erFoedt(tilBoolsk(farskapserklaering.getBarn().getFoedselsnummer() != null))
                .termindato(farskapserklaering.getBarn().getFoedselsnummer() != null ? null : tilDato(farskapserklaering.getBarn().getTermindato()))
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
            .vedlegg(List.of(
                new Vedlegg(new Tekst("PDF"), new Tekst(farskapserklaering.getDokument().getNavn())),
                new Vedlegg(new Tekst("XML"), new Tekst("xadesMor.xml")),
                new Vedlegg(new Tekst("XML"), new Tekst("xadesFar.xml"))))
            .foreldreBorSammen(new Boolsk(true))
            .build())
        .build();
  }

  private void validereFarskapserklaeringKlarTilOversendelse(Farskapserklaering farskapserklaering) {
    try {
      Validate.isTrue(farskapserklaering.getMeldingsidSkatt() != null);
      Validate.isTrue(farskapserklaering.getFar().getFoedselsnummer() != null);
      Validate.isTrue(farskapserklaering.getMor().getFoedselsnummer() != null);
      Validate.isTrue(farskapserklaering.getDokument().getNavn() != null);
      Validate.isTrue(farskapserklaering.getDokument().getDokumentinnhold().getInnhold() != null);
      Validate.isTrue(farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null);
      Validate.isTrue(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null);
    } catch (IllegalArgumentException iae) {
      throw new SkattConsumerException(Feilkode.FARSKAPSERKLAERING_MANGLER_DATA, iae);
    }
  }

  private Dato henteDatoForErklaeringen(Farskapserklaering farskapserklaering) {
    if (farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null) {
      throw new SkattConsumerException(Feilkode.FARSKAPSERKLAERING_MANGLER_SIGNATUR);
    }
    var signeringstidspunktFar = farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt();
    return tilDato(signeringstidspunktFar);
  }

  private DatoKlokkeslett tilDatoKlokkeslett(LocalDateTime localDateTime) {
    return new DatoKlokkeslett(localDateTime.toString());
  }

  private Dato tilDato(LocalDateTime localDateTime) {
    return tilDato(LocalDate.from(localDateTime));
  }

  private Dato tilDato(LocalDate localDate) {
    try {
      var xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDate.from(localDate).toString());
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
