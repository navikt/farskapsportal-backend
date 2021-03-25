package no.nav.farskapsportal.consumer.esignering;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.core.PAdESReference;
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
import no.digipost.signature.client.direct.WithSignerUrl;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.StatusSignering;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.exception.EsigneringConsumerException;
import no.nav.farskapsportal.exception.HentingAvDokumentFeiletException;
import no.nav.farskapsportal.exception.OppretteSigneringsjobbException;
import no.nav.farskapsportal.exception.PadesUrlIkkeTilgjengeligException;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.Signeringsinformasjon;
import org.apache.commons.lang3.Validate;

@Slf4j
@RequiredArgsConstructor
public class DifiESignaturConsumer {

  private final DirectClient client;
  private final FarskapsportalEgenskaper farskapsportalEgenskaper;

  /**
   * Oppretter signeringsjobb hos signeringsløsingen. Oppdaterer dokument med status-url og redirect-urler for signeringspartene.
   *
   * @param dokument dokument med metadata
   * @param mor første signerer
   * @param far andre signerer
   */
  public void oppretteSigneringsjobb(Dokument dokument, Forelder mor, Forelder far) {

    log.info("Oppretter signeringsjobb");

    var document = DirectDocument.builder("Subject", "document.pdf", dokument.getInnhold()).build();

    var exitUrls = ExitUrls
        .of(URI.create(farskapsportalEgenskaper.getEsigneringSuksessUrl()), URI.create(farskapsportalEgenskaper.getEsigneringAvbruttUrl()),
            URI.create(farskapsportalEgenskaper.getEsigneringFeiletUrl()));

    var morSignerer = DirectSigner.withPersonalIdentificationNumber(mor.getFoedselsnummer()).build();
    var farSignerer = DirectSigner.withPersonalIdentificationNumber(far.getFoedselsnummer()).build();

    var directJob = DirectJob.builder(document, exitUrls, List.of(morSignerer, farSignerer)).build();
    DirectJobResponse directJobResponse = null;
    try {
      directJobResponse = client.create(directJob);
    } catch (Exception e) {
      e.printStackTrace();
      throw new OppretteSigneringsjobbException(Feilkode.OPPRETTE_SIGNERINGSJOBB);
    }

    directJobResponse.getSigners().get(0).getSignerUrl();
    log.info("Setter statusUrl {}", directJobResponse.getStatusUrl());
    dokument.setDokumentStatusUrl(directJobResponse.getStatusUrl().toString());

    log.info("Antall signerere i respons: {}", directJob.getSigners().size());

    for (DirectSignerResponse signer : directJobResponse.getSigners()) {
      Validate.notNull(signer.getRedirectUrl(), "Null redirect url mottatt fra PDL!");
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

    var pAdESReference = directJobStatusResponse.getpAdESUrl();
    var statusJobb = directJobStatusResponse.getStatus();
    var bekreftelseslenke = directJobStatusResponse.getConfirmationReference().getConfirmationUrl();

    if (statusJobb.equals(DirectJobStatus.FAILED)) {
      throw new EsigneringConsumerException("Signeringsjobben har status FAILED");
    }

    var signaturer = directJobStatusResponse.getSignatures().stream().filter(Objects::nonNull).map(signatur -> mapTilDto(signatur))
        .collect(Collectors.toList());

    var dokumentstatus = DokumentStatusDto.builder().statuslenke(statuslenke).padeslenke(pAdESReference.getpAdESUrl())
        .bekreftelseslenke(bekreftelseslenke)
        .erSigneringsjobbenFerdig(statusJobb.equals(DirectJobStatus.COMPLETED_SUCCESSFULLY)).signaturer(signaturer).build();

    return dokumentstatus;
  }

  private Map<URI, DirectJobStatusResponse> henteSigneringsjobbstatus(Set<URI> statusUrler, String statusQueryToken) {
    for (URI statusUrl : statusUrler) {
      var directJobResponse = new DirectJobResponse(1, null, statusUrl, null);

      var directJobStatusResponse = client.getStatus(StatusReference.of(directJobResponse).withStatusQueryToken(statusQueryToken));
      if (directJobStatusResponse.isPAdESAvailable()) {
        return Collections.singletonMap(statusUrl, directJobStatusResponse);
      }
    }
    throw new PadesUrlIkkeTilgjengeligException("Pades-url mangler i respons fra signeringsløsningen");
  }

  private void signatureierErIkkeNull(Signature signature) {
    if (signature.getSigner() == null) {
      throw new ESigneringFeilException(Feilkode.ESIGNERING_SIGNATUREIER_NULL);
    }
  }

  public byte[] henteSignertDokument(URI padesUrl) {
    try {
      return client.getPAdES(PAdESReference.of(padesUrl)).readAllBytes();
    } catch (IOException e) {
      throw new HentingAvDokumentFeiletException("Feil oppstod ved lesing av dokument-bytes");
    }
  }

  public URI henteNyRedirectUrl(URI signerUrl) {
    var directSignerResponse = client.requestNewRedirectUrl(WithSignerUrl.of(signerUrl));
    return directSignerResponse.getSignerUrl();
  }

  private SignaturDto mapTilDto(Signature signature) {
    signatureierErIkkeNull(signature);
    var tidspunktForStatus = LocalDateTime.ofInstant(signature.getStatusDateTime(), ZoneOffset.UTC);
    var statusSignering = mapStatus(signature.getStatus());
    var harSignert = StatusSignering.SUKSESS.equals(statusSignering);
    return SignaturDto.builder().signatureier(signature.getSigner()).harSignert(harSignert).statusSignering(statusSignering)
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
