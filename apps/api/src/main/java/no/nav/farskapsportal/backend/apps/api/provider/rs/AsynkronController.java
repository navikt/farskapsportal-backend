package no.nav.farskapsportal.backend.apps.api.provider.rs;

import static no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplication.ISSUER_AZURE_AD;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.dto.asynkroncontroller.HenteAktoeridRequest;
import no.nav.security.token.support.core.api.Protected;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/asynkron")
@ProtectedWithClaims(issuer = ISSUER_AZURE_AD)
public class AsynkronController {

  @Autowired
  private FarskapsportalService farskapsportalService;

  @Autowired
  private PersonopplysningService personopplysningService;

  @PutMapping("/statussynkronisering/farskapserklaering/{id}")
  @Operation(description = "Synkroniserer signeringsstatus for farskapserklæring med signeringsoppdrag i Posten. Oppdaterer status i Farskapsportal hvis endret.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Status synkronisert"),
      @ApiResponse(responseCode = "400", description = "Feil opplysninger oppgitt"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklaering"),
      @ApiResponse(responseCode = "410", description = "Status på signeringsjobben er FEILET. Farskapserklæring slettes og må opprettes på ny."),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<Void> synkronisereSigneringsstatusForFarIFarskapserklaering(
      @Parameter(name = "id", description = "ID til farskapserklæringen status skal oppdateres for") @PathVariable(name = "id") int idFarskapserklaering) {
    farskapsportalService.synkronisereSigneringsstatusFar(idFarskapserklaering);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  @PostMapping("/aktoerid/hente")
  @Operation(description = "Henter aktørid til personident")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Fant og returnerte aktørid for person"),
      @ApiResponse(responseCode = "204", description = "Person mangler aktørid"),
      @ApiResponse(responseCode = "404", description = "Fant ikke person"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<String> henteAktoerid(@Valid @RequestBody HenteAktoeridRequest request) {
    log.info("Henter aktørid for person");
    SIKKER_LOGG.info("Henter aktørid for person med personident {}", request.getPersonident());
    var aktoerid = personopplysningService.henteAktoerid(request.getPersonident());
    return aktoerid.isPresent() ? new ResponseEntity<>(aktoerid.get(), HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
