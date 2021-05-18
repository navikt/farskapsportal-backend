package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.lageUrl;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.api.xml.XMLDirectSignatureJobStatus;
import no.digipost.signature.api.xml.XMLDirectSignatureJobStatusResponse;
import no.digipost.signature.api.xml.XMLDirectSignerStatusValue;
import no.digipost.signature.api.xml.XMLSignerSpecificUrl;
import no.digipost.signature.api.xml.XMLSignerStatus;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Unprotected
@Getter
@Setter
@RequestMapping("/esignering")
@Slf4j
@ActiveProfiles(PROFILE_TEST)
public class EsigneringStubController {

  private final static String FNR_MOR = "12345678910";
  private final static String FNR_FAR = "11111122222";

  private boolean morHarSignert = false;
  private boolean farHarSignert = false;

  @PostMapping(value = "/api/{fnrSignatoer}/direct/signature-jobs/1/redirect")
  public ResponseEntity<Void> signereDokument(@PathVariable("fnrSignatoer") String fnrSignatoer) {
    log.info("Signere dokument for {}", fnrSignatoer);

    var signeringsstatusOppdatert = false;

    if (FNR_MOR.equals(fnrSignatoer)) {
      signeringsstatusOppdatert = morHarSignert == false;
      this.morHarSignert = true;
    } else if (FNR_FAR.equals(fnrSignatoer)) {
      signeringsstatusOppdatert = farHarSignert == false;
      this.farHarSignert = true;
    }

    return signeringsstatusOppdatert ? new ResponseEntity<>(HttpStatus.CREATED) : new ResponseEntity<>(HttpStatus.OK);
  }

  @GetMapping(value = "/api/{fnrSignatoer}/direct/signature-jobs/1/status")
  public ResponseEntity<String> henteStatus(@PathVariable("fnrSignatoer") String fnrSignerer) throws JAXBException {
    log.info("Hente status for signeringsjobb for signerer {}", fnrSignerer);

    XMLSignerStatus signerStatusMor = new XMLSignerStatus();
    signerStatusMor.setSigner(FNR_MOR);
    signerStatusMor.setValue(morHarSignert ? XMLDirectSignerStatusValue.SIGNED : XMLDirectSignerStatusValue.WAITING);
    signerStatusMor.setSince(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("GMT+1")));

    XMLSignerStatus signerStatusFar = new XMLSignerStatus();
    signerStatusFar.setSigner(FNR_FAR);
    signerStatusFar.setValue(farHarSignert ? XMLDirectSignerStatusValue.SIGNED : XMLDirectSignerStatusValue.WAITING);
    signerStatusFar.setSince(ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("GMT+1")));

    XMLDirectSignatureJobStatusResponse statusrespons = new XMLDirectSignatureJobStatusResponse();
    statusrespons.getStatuses().add(signerStatusMor);
    statusrespons.getStatuses().add(signerStatusFar);
    statusrespons.setConfirmationUrl(lageUrl("/confirmation"));
    statusrespons.setSignatureJobStatus(
        morHarSignert && farHarSignert ? XMLDirectSignatureJobStatus.COMPLETED_SUCCESSFULLY : XMLDirectSignatureJobStatus.IN_PROGRESS);
    statusrespons.setPadesUrl(lageUrl("/pades"));
    statusrespons.setSignatureJobId(1);
    statusrespons.setDeleteDocumentsUrl(lageUrl("/delete-docs"));

    statusrespons.getXadesUrls().add(morHarSignert ? new XMLSignerSpecificUrl(lageUrl("/" + FNR_MOR + "/xades"),FNR_MOR) : null);
    statusrespons.getXadesUrls().add(morHarSignert && farHarSignert ? new XMLSignerSpecificUrl(lageUrl("/" + FNR_FAR + "/xades"),FNR_FAR) : null);

    var sw = new StringWriter();

    JAXBContext context = JAXBContext.newInstance(XMLDirectSignatureJobStatusResponse.class);
    Marshaller mar = context.createMarshaller();
    mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    mar.marshal(statusrespons, sw);

    return new ResponseEntity<>(sw.toString(), HttpStatus.OK);
  }
}
