package no.nav.farskapsportal.consumer.esignering;

import static no.digipost.signature.client.direct.DirectJobStatus.FAILED;
import static no.digipost.signature.client.direct.DirectJobStatus.NO_CHANGES;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.core.PAdESReference;
import no.digipost.signature.client.core.XAdESReference;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.direct.DirectDocument;
import no.digipost.signature.client.direct.DirectJob;
import no.digipost.signature.client.direct.DirectJobResponse;
import no.digipost.signature.client.direct.DirectJobStatus;
import no.digipost.signature.client.direct.DirectJobStatusResponse;
import no.digipost.signature.client.direct.DirectSigner;
import no.digipost.signature.client.direct.DirectSignerResponse;
import no.digipost.signature.client.direct.ExitUrls;
import no.digipost.signature.client.direct.Signature;
import no.digipost.signature.client.direct.SignerStatus;
import no.digipost.signature.client.direct.StatusReference;
import no.digipost.signature.client.direct.StatusRetrievalMethod;
import no.digipost.signature.client.direct.WithSignerUrl;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.StatusSignering;
import no.nav.farskapsportal.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.exception.EsigneringConsumerException;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.exception.OppretteSigneringsjobbException;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.Signeringsinformasjon;
import org.apache.commons.lang3.Validate;

@Slf4j
@RequiredArgsConstructor
public class DifiESignaturConsumer {

  private static final String TITTEL_FARSKAPSERKLAERING = "Farskapserklæring";
  private static final String NAVN_FARSKAPSERKLAERINGSDOKUMENT = "farskapserklaering.pdf";

  private final ExitUrls exitUrls;
  private final DirectClient client;

  /**
   * Oppretter signeringsjobb hos signeringsløsingen. Oppdaterer dokument med status-url og redirect-urler for signeringspartene.
   *
   * @param dokument dokument med metadata
   * @param mor første signatør
   * @param far andre signatør
   */
  public void oppretteSigneringsjobb(Dokument dokument, Forelder mor, Forelder far) {

    log.info("Oppretter signeringsjobb");

    var document = DirectDocument.builder(TITTEL_FARSKAPSERKLAERING, NAVN_FARSKAPSERKLAERINGSDOKUMENT, dokument.getDokumentinnhold().getInnhold())
        .build();

    var morSignerer = DirectSigner.withPersonalIdentificationNumber(mor.getFoedselsnummer()).build();
    var farSignerer = DirectSigner.withPersonalIdentificationNumber(far.getFoedselsnummer()).build();

    var directJob = DirectJob.builder(document, exitUrls, List.of(morSignerer, farSignerer)).build();
    DirectJobResponse directJobResponse;
    try {
      directJobResponse = client.create(directJob);
    } catch (Exception e) {
      e.printStackTrace();
      throw new OppretteSigneringsjobbException(Feilkode.OPPRETTE_SIGNERINGSJOBB);
    }

    log.info("Setter statusUrl {}", directJobResponse.getStatusUrl());
    dokument.setDokumentStatusUrl(directJobResponse.getStatusUrl().toString());

    log.info("Antall signatører i respons: {}", directJob.getSigners().size());

    for (DirectSignerResponse signer : directJobResponse.getSigners()) {
      Validate.notNull(signer.getRedirectUrl(), "Null redirect url mottatt fra Esigneringstjenesten!");
      Validate.notNull(signer.getSignerUrl(), "Null signer url mottatt fra Esigneringstjenesten!");
      if (signer.getPersonalIdentificationNumber().equals(mor.getFoedselsnummer())) {
        dokument.setSigneringsinformasjonMor(
            Signeringsinformasjon.builder().undertegnerUrl(signer.getSignerUrl().toString()).redirectUrl(signer.getRedirectUrl().toString()).build());
      } else if (signer.getPersonalIdentificationNumber().equals(far.getFoedselsnummer())) {
        dokument.setSigneringsinformasjonFar(
            Signeringsinformasjon.builder().undertegnerUrl(signer.getSignerUrl().toString()).redirectUrl(signer.getRedirectUrl().toString()).build());
      } else {
        throw new ESigneringFeilException(Feilkode.ESIGNERING_REDIRECTURL_UKJENT);
      }
    }
  }

  /**
   * Hente dokumentstatus.
   */
  public DokumentStatusDto henteStatus(String statusQueryToken, Set<URI> statuslenker) {

    var directJobStatusResponseMap = henteSigneringsjobbstatus(statuslenker, statusQueryToken);

    var statuslenke = directJobStatusResponseMap.keySet().stream().findAny().get();
    var directJobStatusResponse = directJobStatusResponseMap.get(statuslenke);

    validereInnholdStatusrespons(directJobStatusResponse);

    var pAdESReference = directJobStatusResponse.getpAdESUrl();
    var statusJobb = directJobStatusResponse.getStatus();
    var bekreftelseslenke = directJobStatusResponse.getConfirmationReference().getConfirmationUrl();

    var signaturer = directJobStatusResponse.getSignatures().stream().filter(Objects::nonNull).map(this::mapTilDto)
        .collect(Collectors.toList());

    log.info("Antall signaturer i statusrespons: {}", signaturer.size());

    return DokumentStatusDto.builder()
        .statuslenke(statuslenke)
        .statusSignering(henteSigneringsstatus(statusJobb))
        .padeslenke(pAdESReference != null ? pAdESReference.getpAdESUrl() : null)
        .bekreftelseslenke(bekreftelseslenke)
        .signaturer(signaturer).build();
  }

  public byte[] henteSignertDokument(URI padesUrl) {
    try {
      return client.getPAdES(PAdESReference.of(padesUrl)).readAllBytes();
    } catch (IOException e) {
      throw new InternFeilException(Feilkode.PADESURL_FEILFORMATERT);
    }
  }

  public byte[] henteXadesXml(URI xadesUrl) {
    try {
      return client.getXAdES(XAdESReference.of(xadesUrl)).readAllBytes();
    } catch (IOException e) {
      throw new InternFeilException(Feilkode.XADESURL_FEILFORMATERT);
    }
  }

  public URI henteNyRedirectUrl(URI signerUrl) {
    try {
      var directSignerResponse = client.requestNewRedirectUrl(WithSignerUrl.of(signerUrl));
      return directSignerResponse.getRedirectUrl();
    } catch (Exception e) {
      e.printStackTrace();
    }
    throw new InternFeilException(Feilkode.FEIL_ROLLE);
  }

  public Optional<DokumentStatusDto> henteOppdatertStatusPaaSigneringsjobbHvisEndringer(int idFarskapserklaring, byte[] farskapserklaering,
      String fnrMor, String fnrFar) {

    var directSigners = List
        .of(DirectSigner.withPersonalIdentificationNumber(fnrMor).build(), DirectSigner.withPersonalIdentificationNumber(fnrFar).build());
    var directDocument = DirectDocument.builder(TITTEL_FARSKAPSERKLAERING, NAVN_FARSKAPSERKLAERINGSDOKUMENT, farskapserklaering).build();
    var directJob = DirectJob.builder(directDocument, exitUrls, directSigners)
        .retrieveStatusBy(StatusRetrievalMethod.POLLING)
        .build();
    client.create(directJob);
    var directJobStatusResponse = client.getStatusChange();
    if (directJobStatusResponse.is(NO_CHANGES)) {
      log.info("Ingen statusendring på signeringsjobb knyttet til farskapserklæring med id {}", idFarskapserklaring);
      client.confirm(directJobStatusResponse);
      return Optional.empty();
    } else if (directJobStatusResponse.isPAdESAvailable()) {

      var pAdESReference = directJobStatusResponse.getpAdESUrl();
      var statusJobb = directJobStatusResponse.getStatus();
      var bekreftelseslenke = directJobStatusResponse.getConfirmationReference().getConfirmationUrl();

      client.confirm(directJobStatusResponse);

      return Optional.of(DokumentStatusDto.builder()
          .statusSignering(henteSigneringsstatus(statusJobb))
          .padeslenke(pAdESReference.getpAdESUrl())
          .bekreftelseslenke(bekreftelseslenke)
          .build());
    }
    client.confirm(directJobStatusResponse);
    return Optional.empty();
  }

  private void validereInnholdStatusrespons(DirectJobStatusResponse directJobStatusResponse) {

    var signaturer = directJobStatusResponse.getSignatures().stream().filter(Objects::nonNull).map(this::mapTilDto)
        .collect(Collectors.toList());
    try {

      Validate.isTrue(directJobStatusResponse.getStatus() != null, "Statusinformasjon mangler");

      if (!directJobStatusResponse.getStatus().equals(FAILED)) {
        Validate.isTrue(directJobStatusResponse.getConfirmationReference().getConfirmationUrl() != null, "Bekreftelseslenke mangler");
        Validate.isTrue(directJobStatusResponse.getpAdESUrl() != null, "Padeslenke mangler");
        Validate.isTrue(signaturer.size() == 2, "Feil antall singaturer");

        log.info("Antall signaturer i respons fra Posten: {}", signaturer.size());

        Validate.isTrue(signaturer.stream()
            .filter(s -> s.getStatusSignering().equals(StatusSignering.SUKSESS) || s.getStatusSignering().equals(StatusSignering.PAAGAAR))
            .filter(s -> s.getXadeslenke() == null).count() == 0, "Xades-lenke mangler!");
      }
    } catch (IllegalArgumentException iae) {
      throw new EsigneringConsumerException(Feilkode.ESIGNERING_MANGLENDE_DATA, iae);
    }
  }

  private StatusSignering henteSigneringsstatus(DirectJobStatus directJobStatus) {
    switch (directJobStatus) {
      case COMPLETED_SUCCESSFULLY:
        return StatusSignering.SUKSESS;
      case IN_PROGRESS:
        return StatusSignering.PAAGAAR;
      case FAILED:
        return StatusSignering.FEILET;
      case NO_CHANGES:
        return StatusSignering.INGEN_ENDRING;
    }
    throw new InternFeilException(Feilkode.UKJENT_SIGNERINGSSTATUS);
  }

  private Map<URI, DirectJobStatusResponse> henteSigneringsjobbstatus(Set<URI> statusUrler, String statusQueryToken) {
    log.info("Henter status på signeringsjobb. Leter etter riktig status-url ut fra {} mulige kandidater", statusUrler.size());

    for (URI statusUrl : statusUrler) {
      var directJobResponse = new DirectJobResponse(1, null, statusUrl, null);

      var directJobStatusResponse = client.getStatus(StatusReference.of(directJobResponse).withStatusQueryToken(statusQueryToken));
      log.info("Fant riktig status-url");
      return Collections.singletonMap(statusUrl, directJobStatusResponse);
    }

    throw new EsigneringConsumerException(Feilkode.ESIGNERING_UKJENT_TOKEN);
  }

  private void signatureierErIkkeNull(Signature signature) {
    if (signature.getSigner() == null) {
      throw new ESigneringFeilException(Feilkode.ESIGNERING_SIGNATUREIER_NULL);
    }
  }


  private SignaturDto mapTilDto(Signature signature) {
    signatureierErIkkeNull(signature);
    var tidspunktForStatus = LocalDateTime.ofInstant(signature.getStatusDateTime(), ZoneOffset.UTC);
    var statusSignering = mapStatus(signature.getStatus());
    var harSignert = StatusSignering.SUKSESS.equals(statusSignering);
    return SignaturDto.builder()
        .signatureier(signature.getSigner())
        .harSignert(harSignert)
        .xadeslenke(signature.getxAdESUrl() != null ? signature.getxAdESUrl().getxAdESUrl() : null)
        .statusSignering(statusSignering)
        .tidspunktForStatus(tidspunktForStatus).build();
  }

  private StatusSignering mapStatus(SignerStatus signerStatus) {
    if (SignerStatus.SIGNED.equals(signerStatus)) {
      return StatusSignering.SUKSESS;
    } else if (SignerStatus.REJECTED.equals(signerStatus)) {
      return StatusSignering.AVBRUTT;
    } else {
      return StatusSignering.FEILET;
    }
  }

}
