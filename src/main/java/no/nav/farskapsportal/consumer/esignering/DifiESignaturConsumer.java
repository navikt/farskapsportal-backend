package no.nav.farskapsportal.consumer.esignering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.api.xml.XMLDirectSignerResponse;
import no.digipost.signature.client.ClientConfiguration;
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
import no.digipost.signature.client.direct.StatusReference;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.RedirectUrlDto;
import no.nav.farskapsportal.dto.SignaturDto;
import no.nav.farskapsportal.exception.HentingAvDokumentFeiletException;
import no.nav.farskapsportal.exception.PadesUrlIkkeTilgjengeligException;
import no.nav.farskapsportal.exception.SigneringsjobbFeiletException;
import org.apache.commons.lang3.Validate;
import org.modelmapper.ModelMapper;

@Slf4j
@RequiredArgsConstructor
public class DifiESignaturConsumer {

  private final ClientConfiguration clientConfiguration;
  private final ModelMapper modelMapper;
  private final DirectClient client;
  private final boolean disableEsignering;

  /**
   * Oppretter signeringsjobb hos signeringsløsingen. Oppdaterer dokument med status-url og
   * redirect-urler for signeringspartene.
   *
   * @param dokument dokument med metadata
   * @param mor første signerer
   * @param far andre signerer
   */
  public void oppretteSigneringsjobb(DokumentDto dokument, ForelderDto mor, ForelderDto far) {

    log.info("Oppretter signeringsjobb");
    var client = new DirectClient(clientConfiguration);

    var document = DirectDocument.builder("Subject", "document.pdf", dokument.getInnhold()).build();

    var exitUrls =
        ExitUrls.of(
            URI.create("http://nav.no/farskapsportal/onCompletion"),
            URI.create("http://nav.no/farskapsportal/onRejection"),
            URI.create("http://nav.no/farskapsportal/onError"));

    var morSignerer =
        DirectSigner.withPersonalIdentificationNumber(mor.getFoedselsnummer()).build();
    var farSignerer =
        DirectSigner.withPersonalIdentificationNumber(far.getFoedselsnummer()).build();

    var directJob =
        DirectJob.builder(document, exitUrls, List.of(morSignerer, farSignerer)).build();
    DirectJobResponse directJobResponse = null;
    try {
      directJobResponse =
          disableEsignering ? mockDirectJobResponse(directJob) : client.create(directJob);
    } catch (Exception e) {
      e.printStackTrace();
    }
    dokument.setDokumentStatusUrl(directJobResponse.getStatusUrl());

    for (DirectSignerResponse signer : directJobResponse.getSigners()) {
      Validate.notNull(signer.getRedirectUrl(), "Null redirect url mottatt fra PDL!");
      if (signer.getPersonalIdentificationNumber().equals(mor.getFoedselsnummer())) {
        dokument.setRedirectUrlMor(
            RedirectUrlDto.builder().redirectUrl(signer.getRedirectUrl()).signerer(mor).build());
      } else if (signer.getPersonalIdentificationNumber().equals(far.getFoedselsnummer())) {
        dokument.setRedirectUrlFar(
            RedirectUrlDto.builder().redirectUrl(signer.getRedirectUrl()).signerer(far).build());
      } else {
        throw new ESigneringFeilException(
            "Redirecturl for ukjent part mottatt fra signeringsløsningen!");
      }
    }
  }

  private DirectJobResponse mockDirectJobResponse(DirectJob directJob) throws URISyntaxException {
    var signatureJobId = 1000;
    var reference = "1234";
    var statusUrl =
        new URI(
            "https://farskapsportal-esignering-stub.dev.nav.no/api/"
                + directJob.getSigners().stream()
                    .findFirst()
                    .get()
                    .getPersonalIdentificationNumber()
                + "/direct/signature-jobs/1/status");

    var directsigner = directJob.getSigners().get(0);
    directsigner.getPersonalIdentificationNumber();

    var directSignerMor = directJob.getSigners().get(0);
    var directSignerFar = directJob.getSigners().get(1);

    var redirectUrl = "https://farskapsportal.no/redirect";
    var xmlDirectSignerResponseMor =
        new XMLDirectSignerResponse(
            new URI(""),
            directSignerMor.getPersonalIdentificationNumber(),
            "0",
            new URI(redirectUrl + "Mor"));
    var xmlDirectSignerResponseFar =
        new XMLDirectSignerResponse(
            new URI(""),
            directSignerFar.getPersonalIdentificationNumber(),
            "1",
            new URI(redirectUrl + "Far"));

    return new DirectJobResponse(
        signatureJobId,
        reference,
        statusUrl,
        List.of(
            DirectSignerResponse.fromJaxb(xmlDirectSignerResponseMor),
            DirectSignerResponse.fromJaxb(xmlDirectSignerResponseFar)));
  }

  /**
   * Hente dokumentstatus etter at bruker har blitt redirektet med statusQueryToken fra
   * signeringsløsningen.
   *
   * @param statusQueryToken
   * @param statuslenker
   * @return
   */
  public DokumentStatusDto henteDokumentstatusEtterRedirect(
      String statusQueryToken, Set<URI> statuslenker) {

    var directJobStatusResponseMap = henteSigneringsjobbstatus(statuslenker, statusQueryToken);
    var statuslenke = directJobStatusResponseMap.keySet().stream().findAny().get();
    var directJobStatusResponse = directJobStatusResponseMap.get(statuslenke);

    var pAdESReference = directJobStatusResponse.getpAdESUrl();
    var statusJobb = directJobStatusResponse.getStatus();

    if (statusJobb.equals(DirectJobStatus.FAILED)) {
      throw new SigneringsjobbFeiletException("Signeringsjobben har status FAILED");
    }

    var signaturer =
        directJobStatusResponse.getSignatures().stream()
            .filter(Objects::nonNull)
            .map(signatur -> modelMapper.map(signatureierErIkkeNull(signatur), SignaturDto.class))
            .collect(Collectors.toList());

    var dokumentstatus =
        DokumentStatusDto.builder()
            .statuslenke(statuslenke)
            .padeslenke(pAdESReference.getpAdESUrl())
            .erSigneringsjobbenFerdig(statusJobb.equals(DirectJobStatus.COMPLETED_SUCCESSFULLY))
            .signaturer(signaturer)
            .build();

    return dokumentstatus;
  }

  private Map<URI, DirectJobStatusResponse> henteSigneringsjobbstatus(
      Set<URI> statusUrler, String statusQueryToken) {
    for (URI statusUrl : statusUrler) {
      var directJobResponse = new DirectJobResponse(1, null, statusUrl, null);

      var directJobStatusResponse =
          client.getStatus(
              StatusReference.of(directJobResponse).withStatusQueryToken(statusQueryToken));
      if (directJobStatusResponse.isPAdESAvailable()) {
        return Collections.singletonMap(statusUrl, directJobStatusResponse);
      }
    }
    throw new PadesUrlIkkeTilgjengeligException(
        "Pades-url mangler i respons fra signeringsløsningen");
  }

  private Signature signatureierErIkkeNull(Signature signature) {
    if (signature.getSigner() == null) {
      throw new ESigneringFeilException("Signatureier er null i respons fra esigneringsløsningen!");
    }
    return signature;
  }

  public byte[] henteSignertDokument(URI padesUrl) {
    try {
      return client.getPAdES(PAdESReference.of(padesUrl)).readAllBytes();
    } catch (IOException e) {
      throw new HentingAvDokumentFeiletException("Feil oppstod ved lesing av dokument-bytes");
    }
  }
}
