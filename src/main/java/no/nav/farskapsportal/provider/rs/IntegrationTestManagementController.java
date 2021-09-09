package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnExpression("${INTEGRATION_TEST_CONTROLLER_PAA:false}")
@RequestMapping("/dev/integration-test/management")
@ProtectedWithClaims(issuer = ISSUER)
@Slf4j
public class IntegrationTestManagementController {

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private PdfGeneratorConsumer pdfGeneratorConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @PostMapping("/testdata/deaktivere")
  @Operation(description = "Deaktiverer farskapserklæringer. Tilgjengelig kun i DEV.")
  public String deaktivereAlleFarskapserklaeringer() {
    for (Farskapserklaering fe : farskapserklaeringDao.findAll()) {
      persistenceService.deaktivereFarskapserklaering(fe.getId());
    }
    return "Farskapserklæringene i Farskapsportal public-skjema er nå deaktivert";
  }

  @PostMapping("/testdata/deaktivere/{id}")
  @Operation(description = "Deaktiverer farskapserklæringer. Tilgjengelig kun i DEV.")
  public String deaktivereFarskapserklaering(@PathVariable int id) {
    persistenceService.deaktivereFarskapserklaering(id);
    return "Farskapserklæring med id" + id + " er nå deaktivert";
  }

  @GetMapping("/test/farskapserklaering/{idFarskapserklaering}/pades")
  @Operation(description = "Henter PADES for en farskapserklærings forelder")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Dokument hentet uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring eller dokument"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<byte[]> hentePades(@PathVariable int idFarskapserklaering) {
    var fp = farskapserklaeringDao.findById(idFarskapserklaering);
    var innholdPades = fp.get().getDokument().getDokumentinnhold().getInnhold();
    return new ResponseEntity<>(innholdPades, HttpStatus.OK);
  }

  @GetMapping("/test/farskapserklaering/{idFarskapserklaering}/xades/mor")
  @Operation(description = "Henter XADES for en farskapserklærings forelder")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Dokument hentet uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring eller dokument"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<byte[]> henteXadesMor(@PathVariable int idFarskapserklaering) {
    var fp = farskapserklaeringDao.findById(idFarskapserklaering);
    var innholdXades = fp.get().getDokument().getSigneringsinformasjonMor().getXadesXml();
    return new ResponseEntity<>(innholdXades, HttpStatus.OK);
  }

  @GetMapping("/test/farskapserklaering/{idFarskapserklaering}/xades/far")
  @Operation(description = "Henter XADES for en farskapserklærings forelder")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Dokument hentet uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring eller dokument"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<byte[]> henteXadesFar(@PathVariable int idFarskapserklaering) {
    var fp = farskapserklaeringDao.findById(idFarskapserklaering);
    var innholdXades = fp.get().getDokument().getSigneringsinformasjonFar().getXadesXml();
    return new ResponseEntity<>(innholdXades, HttpStatus.OK);
  }

  @GetMapping("/test/pdf")
  @Operation(description = "Henter test-PDF")
  public ResponseEntity<byte[]> hentePdf() {

    var barn = BarnDto.builder().termindato(LocalDate.now().plusMonths(1)).build();

    var mor = ForelderDto.builder()
        .foedselsnummer("11046000201")
        .foedselsdato(LocalDate.now().minusYears(26))
        .navn(NavnDto.builder().fornavn("Bambi").etternavn("Normann").build()).build();

    var far = ForelderDto.builder()
        .foedselsnummer("11029400522")
        .foedselsdato(LocalDate.now().minusYears(26))
        .navn(NavnDto.builder().fornavn("Bamse").etternavn("Normann").build()).build();

    var pdf = pdfGeneratorConsumer.genererePdf(barn, mor, far, null);

    return new ResponseEntity<>(pdf, HttpStatus.OK);
  }
}
