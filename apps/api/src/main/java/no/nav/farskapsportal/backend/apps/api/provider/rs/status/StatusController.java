package no.nav.farskapsportal.backend.apps.api.provider.rs.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.ActuatorHealth;
import no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.OperasjonellStatus;
import no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.Status;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Validated
@RestController
@Unprotected
@RequestMapping("/internal/farskapsportal")
public class StatusController {

  private RestTemplate restTemplate;

  public StatusController() {
    var thisRestTemplate = new RestTemplate();
    thisRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler("http://localhost:8080/"));
    this.restTemplate = thisRestTemplate;
  }

  @GetMapping(value = "/status")
  @Operation(description = "Avgjør foreldrerolle til person. Henter ventende farskapserklæringer. Henter nyfødte barn",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Statusinformasjon hentet"),
      @ApiResponse(responseCode = "400", description = "Ugyldig request"),
      @ApiResponse(responseCode = "404", description = "Fant ikke statusinformasjon"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<OperasjonellStatus> henteStatus() {

    var helsesjekk = restTemplate.exchange(
        "/internal/actuator/health",
        HttpMethod.GET,
        new HttpEntity<>(null, null),
        ActuatorHealth.class);

    var actuatorHealth = helsesjekk.getBody();

    var operasjonellStatus = actuatorHealth.getStatus().equals(Status.UP) ? Systemstatus.OK : Systemstatus.ERROR;

    return new ResponseEntity<>(OperasjonellStatus.builder()
        .name("Farskapsportal")
        .status(operasjonellStatus.toString())
        .team("Bidrag")
        .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:MM:ss.SSS z")))
        .build(), HttpStatus.OK);
  }

  enum Systemstatus {
    OK, WARN, ERROR
  }

}
