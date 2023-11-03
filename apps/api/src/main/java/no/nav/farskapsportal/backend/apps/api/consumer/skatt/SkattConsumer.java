package no.nav.farskapsportal.backend.apps.api.consumer.skatt;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
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
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.apache.commons.lang3.Validate;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.ByteArrayBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Slf4j
@AllArgsConstructor
public class SkattConsumer {

  private static String AVSENDER_KILDESYSTEM = "FARSKAPSPORTAL";
  private final CloseableHttpClient httpClient;
  private final ConsumerEndpoint consumerEndpoint;
  private BucketConsumer bucketConsumer;

  @Retryable(value = IOException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000000))
  public LocalDateTime registrereFarskap(Farskapserklaering farskapserklaering) {

    log.info(
        "Registrerer farskap for erklæring med meldingsid {}",
        farskapserklaering.getMeldingsidSkatt());

    var meldingOmRegistreringAvFarskap = tilMeldingOmRegistreringAvFarskap(farskapserklaering);
    var xml = tilStreng(meldingOmRegistreringAvFarskap);

    var innholdPades =
        bucketConsumer.getContentFromBucket(farskapserklaering.getDokument().getBlobIdGcp());
    if (innholdPades != null && innholdPades.length < 1) {
      throw new SkattConsumerException(Feilkode.DOKUMENT_MANGLER_INNOHLD);
    }

    var innholdXadesMor =
        bucketConsumer.getContentFromBucket(
            farskapserklaering.getDokument().getSigneringsinformasjonMor().getBlobIdGcp());
    if (innholdXadesMor != null && innholdXadesMor.length < 1) {
      throw new SkattConsumerException((Feilkode.XADES_MOR_UTEN_INNHOLD));
    }

    var innholdXadesFar =
        bucketConsumer.getContentFromBucket(
            farskapserklaering.getDokument().getSigneringsinformasjonFar().getBlobIdGcp());
    if (innholdXadesFar != null && innholdXadesFar.length < 1) {
      throw new SkattConsumerException((Feilkode.XADES_FAR_UTEN_INNHOLD));
    }

    try {

      final var post =
          new HttpPost(consumerEndpoint.retrieveEndpoint(SkattEndpoint.MOTTA_FARSKAPSERKLAERING));

      // Melding - Metadata som beskriver forsendelsen
      final var melding = new StringBody(xml, ContentType.APPLICATION_XML);
      // Ferdig signert dokument. Inneholder både mor og fars XADES-signeringsfiler
      final var pades =
          new ByteArrayBody(
              innholdPades,
              ContentType.APPLICATION_PDF,
              farskapserklaering.getDokument().getNavn());
      final var xadesMor =
          new ByteArrayBody(innholdXadesMor, ContentType.APPLICATION_XML, "xadesMor.xml");
      final var xadesFar =
          new ByteArrayBody(innholdXadesFar, ContentType.APPLICATION_XML, "xadesFar.xml");
      final var reqEntity =
          MultipartEntityBuilder.create()
              .addPart("melding", melding)
              .addPart("vedlegg", pades)
              .addPart("melding2", xadesMor)
              .addPart("melding3", xadesFar)
              .build();

      log.info("Executing request " + post.getMethod() + " " + post.getUri());

      post.setEntity(reqEntity);

      httpClient.execute(
          post,
          response -> {
            if (HttpStatus.ACCEPTED.value() != response.getCode()) {
              log.error(
                  "Mottok Http-kode {}, ved overføring av farskapserklæring med meldingsid {} til Skatt",
                  response.getCode(),
                  farskapserklaering.getMeldingsidSkatt());
              throw new SkattConsumerException(Feilkode.SKATT_OVERFOERING_FEILET);
            }
            return null;
          });

      return LocalDateTime.parse(
          meldingOmRegistreringAvFarskap
              .getInnsending()
              .getAvsendersInnsendingstidspunkt()
              .getDateTime());

    } catch (Exception e) {
      e.printStackTrace();
      throw new SkattConsumerException(Feilkode.SKATT_OVERFOERING_FEILET, e);
    }
  }

  private String tilStreng(MeldingOmRegistreringAvFarskap meldingOmRegistreringAvFarskap) {
    try {
      var xmlString = new StringWriter();
      Marshaller marshaller =
          JAXBContext.newInstance(MeldingOmRegistreringAvFarskap.class).createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(meldingOmRegistreringAvFarskap, xmlString);
      return xmlString.toString();
    } catch (JAXBException jaxbe) {
      throw new SkattConsumerException(Feilkode.SKATT_MELDINGSFORMAT, jaxbe);
    }
  }

  private MeldingOmRegistreringAvFarskap tilMeldingOmRegistreringAvFarskap(
      Farskapserklaering farskapserklaering) {

    validereFarskapserklaeringKlarTilOversendelse(farskapserklaering);

    return MeldingOmRegistreringAvFarskap.builder()
        .innsending(
            Innsending.builder()
                .avsendersInnsendingstidspunkt(tilDatoKlokkeslett(LocalDateTime.now()))
                .kildesystem(new Tekst(AVSENDER_KILDESYSTEM))
                .avsendersMeldingsidentifikator(new Tekst(farskapserklaering.getMeldingsidSkatt()))
                .build())
        .forespoersel(
            ForespoerselOmRegistreringAvFarskap.builder()
                .far(
                    Far.builder()
                        .datoForErklaeringen(henteDatoForErklaeringen(farskapserklaering))
                        .foedselsEllerDNummer(
                            tilFoedsedslsnummer(farskapserklaering.getFar().getFoedselsnummer()))
                        .harSignert(
                            tilBoolsk(
                                farskapserklaering
                                        .getDokument()
                                        .getSigneringsinformasjonFar()
                                        .getSigneringstidspunkt()
                                    != null))
                        .build())
                .mor(
                    Mor.builder()
                        .foedselsEllerDNummer(
                            tilFoedsedslsnummer(farskapserklaering.getMor().getFoedselsnummer()))
                        .harSignert(
                            tilBoolsk(
                                farskapserklaering
                                        .getDokument()
                                        .getSigneringsinformasjonMor()
                                        .getSigneringstidspunkt()
                                    != null))
                        .build())
                .barnet(
                    Barn.builder()
                        .erFoedt(
                            tilBoolsk(farskapserklaering.getBarn().getFoedselsnummer() != null))
                        .termindato(
                            farskapserklaering.getBarn().getFoedselsnummer() != null
                                ? null
                                : tilDato(farskapserklaering.getBarn().getTermindato()))
                        .foedselsEllerDNummer(
                            farskapserklaering.getBarn().getFoedselsnummer() != null
                                ? tilFoedsedslsnummer(
                                    farskapserklaering.getBarn().getFoedselsnummer())
                                : null)
                        .build())
                .registreringsdato(tilDato(LocalDate.now()))
                .innsender(new Innsender(new InnsendertypeForRegistreringAvFarskap("nav")))
                .mottak(
                    Informasjonsmottak.builder()
                        .informasjonskanal(new KanalForRegistreringAvFarskap("elektroniskMelding"))
                        .mottakstidspunktFraOpprinneligkanal(
                            tilDato(
                                farskapserklaering
                                    .getDokument()
                                    .getSigneringsinformasjonMor()
                                    .getSigneringstidspunkt()))
                        .build())
                .saksbehandlersVurdering(
                    SaksbehandlersVurdering.builder()
                        .skjemaErAttestert(tilBoolsk(true))
                        .vedlagtFarskapsskjemaErOriginalt(tilBoolsk(true))
                        .build())
                .vedlegg(
                    List.of(
                        new Vedlegg(
                            new Tekst("PDF"),
                            new Tekst(farskapserklaering.getDokument().getNavn())),
                        new Vedlegg(new Tekst("XML"), new Tekst("xadesMor.xml")),
                        new Vedlegg(new Tekst("XML"), new Tekst("xadesFar.xml"))))
                .foreldreBorSammen(new Boolsk(true))
                .build())
        .build();
  }

  private void validereFarskapserklaeringKlarTilOversendelse(
      Farskapserklaering farskapserklaering) {
    try {
      Validate.isTrue(farskapserklaering.getFar().getFoedselsnummer() != null);
      Validate.isTrue(farskapserklaering.getMor().getFoedselsnummer() != null);
      Validate.isTrue(farskapserklaering.getDokument().getNavn() != null);

      Validate.isTrue(farskapserklaering.getDokument().getBlobIdGcp() != null);
      Validate.isTrue(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()
              != null);
      Validate.isTrue(
          farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()
              != null);
    } catch (IllegalArgumentException iae) {
      throw new SkattConsumerException(Feilkode.FARSKAPSERKLAERING_MANGLER_DATA, iae);
    }
  }

  private Dato henteDatoForErklaeringen(Farskapserklaering farskapserklaering) {
    if (farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()
        == null) {
      throw new SkattConsumerException(Feilkode.FARSKAPSERKLAERING_MANGLER_SIGNATUR);
    }
    var signeringstidspunktFar =
        farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt();
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
      var xmlGregorianCalendar =
          DatatypeFactory.newInstance()
              .newXMLGregorianCalendar(LocalDate.from(localDate).toString());
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
