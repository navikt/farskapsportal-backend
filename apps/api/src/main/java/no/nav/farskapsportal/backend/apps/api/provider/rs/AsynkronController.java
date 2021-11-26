package no.nav.farskapsportal.backend.apps.api.provider.rs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/asynkron")
@Unprotected
public class AsynkronController {

  @Autowired
  private FarskapsportalService farskapsportalService;

  @PutMapping("/statussynkronisering/farskapserklaering/{id}")
  @Operation(description = "Synkroniserer signeringsstatus for farskapserklæring med signeringsoppdrag i Posten. Oppdaterer status i Farskapsportal hvis endret.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Status synkronisert"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklaering"),
      @ApiResponse(responseCode = "410", description = "Status på signeringsjobben er FEILET. Farskapserklæring slettes og må opprettes på ny."),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<Void> synkronisereSigneringsstatusForFarIFarskapserklaering(
      @Parameter(name = "id", description = "ID til farskapserklæringen status skal oppdateres for") @PathVariable(name = "id") int idFarskapserklaering) {
    farskapsportalService.synkronisereSigneringsstatusFar(idFarskapserklaering);
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }
}
