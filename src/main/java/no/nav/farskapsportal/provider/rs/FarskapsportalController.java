package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.service.FarskapsportalService;
import no.nav.farskapsportal.service.PersonopplysningService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farskapsportal")
@ProtectedWithClaims(issuer = ISSUER)
@Slf4j
public class FarskapsportalController {

  @Autowired private FarskapsportalService farskapsportalService;

  @Autowired private PersonopplysningService personopplysningService;

  @Autowired private OidcTokenSubjectExtractor oidcTokenSubjectExtractor;

  @GetMapping("/brukerinformasjon")
  @ApiOperation(
      "Avgjør foreldrerolle til person. Henter ventende farskapserklæringer. Henter nyfødte barn")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Ingen feil ved bestemming av rolle"),
        @ApiResponse(code = 400, message = "Ugyldig fødselsnummer"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer"),
        @ApiResponse(code = 500, message = "Serverfeil"),
        @ApiResponse(code = 503, message = "Tjeneste utilgjengelig")
      })
  public ResponseEntity<BrukerinformasjonResponse> henteBrukerinformasjon() {
    log.info("Henter brukerinformasjon");
    var brukerinformasjon =
        farskapsportalService.henteBrukerinformasjon(
            oidcTokenSubjectExtractor.hentPaaloggetPerson());

    return new ResponseEntity<>(brukerinformasjon, HttpStatus.OK);
  }

  @PostMapping("/personopplysninger/far")
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
        @ApiResponse(code = 500, message = "Serverfeil"),
        @ApiResponse(code = 503, message = "Tjeneste utilgjengelig")
      })
  public ResponseEntity<Void> kontrollereOpplysningerFar(
      @RequestBody KontrollerePersonopplysningerRequest request) {
    log.info("Starter kontroll av personopplysninger");
    personopplysningService.riktigNavnOgRolle(request, Forelderrolle.FAR);
    log.info("Kontroll av personopplysninger fullført uten feil");
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/farskapserklaering/ny")
  @ApiOperation("Oppretter farskapserklæring")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Farskapserklæring opprettet"),
        @ApiResponse(code = 400, message = "Feil opplysinger oppgitt"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
        @ApiResponse(code = 500, message = "Serverfeil"),
        @ApiResponse(code = 503, message = "Tjeneste utilgjengelig")
      })
  public ResponseEntity<URI> nyFarskapserklaering(
      @RequestBody OppretteFarskaperklaeringRequest request) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var respons = farskapsportalService.oppretteFarskapserklaering(fnrPaaloggetPerson, request);
    var redirectUrlMor = respons.getRedirectUrlForSigneringMor();

    return new ResponseEntity<>(redirectUrlMor, HttpStatus.OK);
  }

  @PutMapping("/farskapserklaering/redirect")
  @ApiOperation(
      "Kalles etter redirect fra singeringsløsningen. Henter kopi av signert dokument fra "
          + "dokumentlager for pålogget person. Lagrer padeslenke. Oppdaterer signeringsstatus.")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Dokumentet ble hentet, og padeslenke lagret uten feil"),
        @ApiResponse(code = 400, message = "Feil opplysinger oppgitt"),
        @ApiResponse(
            code = 401,
            message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
        @ApiResponse(code = 404, message = "Fant ikke dokument"),
        @ApiResponse(code = 500, message = "Serverfeil"),
        @ApiResponse(code = 503, message = "Tjeneste utilgjengelig")
      })
  public ResponseEntity<byte[]> henteDokumentEtterRedirect(
      @ApiParam(
              name = "statusQueryToken",
              type = "String",
              value = "status_query_token som mottatt fra e-signeringsløsningen i redirect-url",
              required = true)
          @RequestParam(name = "status_query_token")
          String statusQuerytoken) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var signertDokument =
        farskapsportalService.henteSignertDokumentEtterRedirect(
            fnrPaaloggetPerson, statusQuerytoken);
    return new ResponseEntity<>(signertDokument, HttpStatus.OK);
  }
}
