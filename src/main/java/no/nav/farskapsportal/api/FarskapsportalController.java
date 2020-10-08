package no.nav.farskapsportal.api;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.service.FarskapsportalService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farskapsportal")
@ProtectedWithClaims(issuer = ISSUER)
@CrossOrigin(
    origins = {
      "https://farskapsportal.nav.no",
      "https://farskapsportal.dev.adeo.no",
      "https://farskapsportal-feature.dev.adeo.no"
    })
@Slf4j
public class FarskapsportalController {

  @Autowired private FarskapsportalService farskapsportalService;

  @Autowired private OidcTokenSubjectExtractor oidcTokenSubjectExtractor;

  @Deprecated
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

  @GetMapping("/rolle")
  @ApiOperation("Bestemmer rolle til person")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Ingen feil ved bestemming av rolle"),
          @ApiResponse(code = 400, message = "Ugyldig fødselsnummer"),
          @ApiResponse(
              code = 401,
              message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
          @ApiResponse(code = 404, message = "Fant ikke fødselsnummer"),
          @ApiResponse(code = 503, message = "Bestemming av rolle for fødselsnummer feilet")
      })
  public ResponseEntity<Kjoenn> bestemmeRolle() {
    log.info("Bestemmer rolle til person");

    return farskapsportalService
        .henteKjoenn(oidcTokenSubjectExtractor.hentPaaloggetPerson())
        .getResponseEntity();
  }

  @PostMapping("/kontrollere/far")
  @ApiOperation(
      "Kontrollerer om fødeslnummer til oppgitt far stemmer med navn; samt at far er mann")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Oppgitt far er mann. Navn og fødselsnummer stemmer"),
        @ApiResponse(
            code = 400,
            message =
                "Ugyldig fødselsnummer, feil kombinasjon av fødselsnummer og navn, eller oppgitt far ikke er mann"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
        @ApiResponse(code = 503, message = "Kontroll av fødselsnummer mot navn feilet")
      })
  public ResponseEntity<Void> kontrollereOpplysningerFar(
      @RequestBody KontrollerePersonopplysningerRequest request) {
    log.info("Starter kontroll av personopplysninger");

    farskapsportalService.riktigNavnOgKjoennOppgittForFar(request);

    log.info("Kontroll av personopplysninger fullført uten feil");

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/farskap/opprette")
  @ApiOperation("Opprette erklæring om farskap til barn")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Farskapserklæring opprettet"),
        @ApiResponse(code = 400, message = "Feil opplysinger oppgitt"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
        @ApiResponse(code = 503, message = "Oppretting av farskap feilet")
      })
  public ResponseEntity<Void> oppretteFarskapserklaering(
      @RequestBody OppretteFarskaperklaeringRequest request) {
    log.info("Oppretter farskap");
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var respons = farskapsportalService.oppretteFarskapserklaering(fnrPaaloggetPerson, request);
    log.info("Erklæring av farskap fullført");
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/farskap/erklaere")
  @ApiOperation("Far erklærer farskap til barn")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "E"),
          @ApiResponse(code = 400, message = "Feil opplysinger oppgitt"),
          @ApiResponse(
              code = 401,
              message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
          @ApiResponse(code = 404, message = "Fant ikke ventende farskapserklæring"),
          @ApiResponse(code = 503, message = "Erklæring av farskap feilet")
      })
  public ResponseEntity<Void> erklaereFarskap(
      @RequestBody BarnDto barnDto) {
    log.info("Oppretter farskap");
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    farskapsportalService.erklaereFarskap(fnrPaaloggetPerson, barnDto);
    log.info("Erklæring av farskap fullført");
    return new ResponseEntity<>(HttpStatus.OK);
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
  public ResponseEntity<Farskapserklaering> henteFarskapserklaeringUnderBehandling(
      @PathVariable String fodselsnummer, @PathVariable String termindato) {
    log.info("Hente farskapserklæring for barn med termindato {}", termindato);

    return new ResponseEntity<>(null, HttpStatus.OK);
  }
}
