package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.DokumentinnholdDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.persistence.dao.SigneringsinformasjonDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnExpression("${INTEGRATION_TEST_CONTROLLER_PAA:false}")
@RequestMapping("/dev/integration-test/management")
@ProtectedWithClaims(issuer = ISSUER)
@Slf4j
public class IntegrationTestManagementController {

  @Autowired
  private BarnDao barnDao;

  @Autowired
  private DokumentDao dokumentDao;

  @Autowired
  private DokumentinnholdDao dokumentinnholdDao;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private ForelderDao forelderDao;

  @Autowired
  private StatusKontrollereFarDao statusKontrollereFarDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  @Autowired
  private SigneringsinformasjonDao signeringsinformasjonDao;

  @Autowired
  private PdfGeneratorConsumer pdfGeneratorConsumer;

  @GetMapping("/testdata/slette")
  @Operation(description = "Sletter lokale testdata. Tilgjengelig kun i DEV.")
  public String sletteTestdata() {

    try {
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      dokumentDao.deleteAll();
      dokumentinnholdDao.deleteAll();
      barnDao.deleteAll();
      signeringsinformasjonDao.deleteAll();
      meldingsloggDao.deleteAll();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return "Testdata slettet fra Farskapsportal public-skjema";
  }

  @GetMapping("/test/farskapserklaering/{idFarskapserklaering}/xades")
  @Operation(description = "Henter XADES for en farskapserklærings forelder")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Dokument hentet uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring eller dokument"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<byte[]> henteXades(@PathVariable int idFarskapserklaering) {
    var fp = farskapserklaeringDao.findById(idFarskapserklaering);
    var innholdXades = fp.get().getDokument().getSigneringsinformasjonMor().getXadesXml();
    return new ResponseEntity<>(innholdXades, HttpStatus.OK);
  }

  @GetMapping("/test/pdf")
  @Operation(description = "Henter test-PDF")
  public ResponseEntity<byte[]> hentePdf() {

    var barn = BarnDto.builder().termindato(LocalDate.now().plusMonths(1)).build();
    var mor = ForelderDto.builder().foedselsnummer("11046000201").fornavn("Bambi").etternavn("Normann").foedselsdato(LocalDate.now().minusYears(26)).build();
    var far  = ForelderDto.builder().foedselsnummer("11029400522").fornavn("Bamse").etternavn("Normann").foedselsdato(LocalDate.now().minusYears(26)).build();

    var pdf = pdfGeneratorConsumer.genererePdf(barn, mor, far);
    return new ResponseEntity<>(pdf, HttpStatus.OK);
  }
}
