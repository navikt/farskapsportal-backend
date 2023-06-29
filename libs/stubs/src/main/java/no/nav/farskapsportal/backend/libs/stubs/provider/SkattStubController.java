package no.nav.farskapsportal.backend.libs.stubs.provider;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.time.LocalDate;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.skatt.api.MeldingOmRegistreringAvFarskap;
import no.nav.security.token.support.core.api.Unprotected;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@Profile("${!live}")
@RequestMapping("/folkeregisteret/mottak/api")
public class SkattStubController {

  @Unprotected
  @PostMapping(
      value = "/registrering_av_farskap_v1.vedlegg",
      consumes = {
        MediaType.MULTIPART_FORM_DATA_VALUE,
        MediaType.APPLICATION_PDF_VALUE,
        MediaType.APPLICATION_XML_VALUE
      })
  public ResponseEntity<Void> registrereFarskap(
      @RequestParam("melding") String xml,
      @RequestParam("vedlegg") MultipartFile vedlegg,
      @RequestParam("melding2") MultipartFile vedlegg2,
      @RequestParam("melding3") MultipartFile vedlegg3)
      throws IOException, JAXBException {
    log.info("Vedlegg og Melding");
    var dokument = vedlegg.getBytes();

    JAXBContext context = JAXBContext.newInstance(MeldingOmRegistreringAvFarskap.class);
    var meldingOmRegistreringAvFarskap =
        (MeldingOmRegistreringAvFarskap)
            context.createUnmarshaller().unmarshal(IOUtils.toInputStream(xml));

    return LocalDate.now()
                .equals(
                    tilLocalDate(
                        meldingOmRegistreringAvFarskap
                            .getForespoersel()
                            .getRegistreringsdato()
                            .getDate()))
            && dokument != null
            && dokument.length > 0
            && vedlegg != null
            && vedlegg.getSize() > 0
            && vedlegg2 != null
            && !vedlegg2.isEmpty()
            && vedlegg3 != null
            && !vedlegg3.isEmpty()
        ? new ResponseEntity<>(HttpStatus.ACCEPTED)
        : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  private LocalDate tilLocalDate(XMLGregorianCalendar xmlGregorianCalendar) {
    return LocalDate.of(
        xmlGregorianCalendar.getYear(),
        xmlGregorianCalendar.getMonth(),
        xmlGregorianCalendar.getDay());
  }
}
