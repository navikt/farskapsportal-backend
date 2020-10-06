package no.nav.farskapsportal.controller;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.BekrefteFarskapRequest;
import no.nav.farskapsportal.api.BekrefteFarskapResponse;
import no.nav.farskapsportal.api.Farskapserklaring;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerResponse;
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.service.FarskapsportalService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farskapsportal")
@ProtectedWithClaims(issuer = ISSUER)
@Slf4j
public class FarskapsportalController {

  @Autowired private FarskapsportalService farskapsportalService;

  @Autowired private OidcTokenSubjectExtractor oidcTokenSubjectExtractor;

  @GetMapping("/kjoenn")
  @ApiOperation("Avgjør kjønn til person")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Ingen feil ved henting av kjønn"),
        @ApiResponse(code = 400, message = "Ugyldig fødselsnummer"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer"),
        @ApiResponse(code = 503, message = "Henting av kjønn for fødselsnummer feilet")
      })
  public ResponseEntity<Kjoenn> henteKjonn() {
    log.info("Henter kjønn til person");

    return farskapsportalService
        .henteKjoenn(oidcTokenSubjectExtractor.hentPaaloggetPerson())
        .getResponseEntity();
  }

  @PostMapping("/kontrollere/far")
  @ApiOperation("Kontrollerer om fødeslnummer til oppgitt far stemmer med navn")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Oppgitt fødselsnummer stemmer med navn"),
        @ApiResponse(
            code = 400,
            message =
                "Ugyldig fødselsnummer, kombinasjon av fødselsnummer og navn, eller at personen ikke er hankjønn"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
        @ApiResponse(code = 503, message = "Kontroll av fødselsnummer mot navn feilet")
      })
  public ResponseEntity<KontrollerePersonopplysningerResponse> kontrollereOpplysningerFar(
      @RequestBody KontrollerePersonopplysningerRequest request) {
    log.info("Starter kontroll av personopplysninger");
    var kontrollerePersonopplysningerResponse = new KontrollerePersonopplysningerResponse();

    farskapsportalService.riktigNavnOppgittForFar(request);

    log.info("Kontroll av personopplysninger fullført");
    return new ResponseEntity<>(kontrollerePersonopplysningerResponse, HttpStatus.OK);
  }

  @PostMapping("/farskap/bekreft")
  @ApiOperation("Bekrefter farskap til barn under svangerskap")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Farskap bekreftet"),
        @ApiResponse(code = 400, message = "Feil opplysinger angitt"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
        @ApiResponse(code = 503, message = "Bekrefting av farskap feilet")
      })
  public ResponseEntity<BekrefteFarskapResponse> bekrefteFarskap(
      @RequestBody BekrefteFarskapRequest request) {
    log.info("Starter kontroll av personopplysninger");
    var bekrefteFarskapResponse = new BekrefteFarskapResponse();

    log.info("Kontroll av personopplysninger fullført");
    return new ResponseEntity<>(bekrefteFarskapResponse, HttpStatus.OK);
  }

  @GetMapping("/erklaering/termindato/{termindato}")
  @ApiOperation("Henter farskapserklæring under behandling som venter på fars signatur")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Farskapserklæring hentet"),
        @ApiResponse(code = 400, message = "Feil opplysinger angitt"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
        @ApiResponse(code = 503, message = "Bekrefting av farskap feilet")
      })
  public ResponseEntity<Farskapserklaring> henteFarskapserklaringUnderBehandling(
      @PathVariable String fodselsnummer, @PathVariable String termindato) {
    log.info("Hente farskapserklæring for barn med termindato {}", termindato);

    var farskapserklaring = new Farskapserklaring();

    return new ResponseEntity<>(farskapserklaring, HttpStatus.OK);
  }
}
