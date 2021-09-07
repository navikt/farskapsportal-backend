package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppdatereFarskapserklaeringRequest;
import no.nav.farskapsportal.api.OppdatereFarskapserklaeringResponse;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringRequest;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.service.FarskapsportalService;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/farskapsportal")
@ProtectedWithClaims(issuer = ISSUER)
public class FarskapsportalController {

  @Autowired
  private FarskapsportalService farskapsportalService;

  @Autowired
  private OidcTokenSubjectExtractor oidcTokenSubjectExtractor;

  @GetMapping("/brukerinformasjon")
  @Operation(description = "Avgjør foreldrerolle til person. Henter ventende farskapserklæringer. Henter nyfødte barn",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ingen feil ved bestemming av rolle"),
      @ApiResponse(responseCode = "400", description = "Ugyldig fødselsnummer"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke fødselsnummer"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<BrukerinformasjonResponse> henteBrukerinformasjon() {
    log.info("Henter brukerinformasjon");
    var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(oidcTokenSubjectExtractor.hentPaaloggetPerson());
    return new ResponseEntity<>(brukerinformasjon, HttpStatus.OK);
  }

  @PostMapping("/personopplysninger/far")
  @Operation(description = "Kontrollerer om fødeslnummer til oppgitt far stemmer med navn; samt at far er mann",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Oppgitt far er mann. Navn og fødselsnummer stemmer"),
      @ApiResponse(responseCode = "400", description = "Ugyldig fødselsnummer, feil kombinasjon av fødselsnummer og navn, eller oppgitt far ikke er mann"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke fødselsnummer eller navn"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<Feilkode> kontrollereOpplysningerFar(@Valid @RequestBody KontrollerePersonopplysningerRequest request) {
    log.info("Starter kontroll av personopplysninger");
    var fnrMor = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    farskapsportalService.validereMor(fnrMor);
    farskapsportalService.kontrollereFar(fnrMor, request);
    log.info("Kontroll av personopplysninger fullført uten feil");
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/farskapserklaering/ny")
  @Operation(description = "Oppretter farskapserklæring",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Farskapserklæring opprettet"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke fødselsnummer eller navn"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<OppretteFarskapserklaeringResponse> nyFarskapserklaering(@Valid @RequestBody OppretteFarskapserklaeringRequest request) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();

    // Sjekker om mor har oppgitt riktige opplysninger om far, samt at far tilfredsstiller krav til digital erklæering
   farskapsportalService.kontrollereFar(fnrPaaloggetPerson, request.getOpplysningerOmFar());
    var respons = farskapsportalService.oppretteFarskapserklaering(fnrPaaloggetPerson, request);
    return new ResponseEntity(respons, HttpStatus.OK);
  }

  @PutMapping("/farskapserklaering/redirect")
  @Operation(description =
      "Kalles etter redirect fra singeringsløsningen. Oppdaterer status på signeringsjobben. Henter kopi av signert dokument fra "
          + "dokumentlager for pålogget person. Lagrer padeslenke. Dersom signeringsjobben har status feilet hos Posten deaktiveres aktuell"
          + "farskapserklæring.",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Status ble oppdatert, og padeslenke lagret uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke dokument"),
      @ApiResponse(responseCode = "410", description = "Status på signeringsjobben er FEILET. Farskapserklæring slettes og må opprettes på ny."),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<FarskapserklaeringDto> oppdatereStatusEtterRedirect(
      @Parameter(name = "status_query_token", description = "statusQueryToken som mottatt fra e-signeringsløsningen i redirect-url", required = true) @RequestParam(name = "status_query_token") String statusQueryToken) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var signertDokument = farskapsportalService.oppdatereStatusSigneringsjobb(fnrPaaloggetPerson, statusQueryToken);
    return new ResponseEntity<>(signertDokument, HttpStatus.OK);
  }

  @PostMapping("/redirect-url/ny")
  @Operation(description = "Henter ny redirect-url for signering av dokument",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ny redirect-url ble hentet uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysninger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<String> henteNyRedirectUrl(
      @Parameter(name = "id_farskapserklaering", description = "Id til aktuell farskapserklaering", required = true) @RequestParam(name = "id_farskapserklaering") int idFarskapserklaering) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var redirectUrl = farskapsportalService.henteNyRedirectUrl(fnrPaaloggetPerson, idFarskapserklaering);
    return new ResponseEntity<>(redirectUrl.toString(), HttpStatus.OK);
  }

  @PutMapping("/farskapserklaering/oppdatere")
  @Operation(description = "Oppdaterer farskapserklæring",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Farskapserklæring oppdatert uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<OppdatereFarskapserklaeringResponse> oppdatereFarskapserklaering(@Valid @RequestBody OppdatereFarskapserklaeringRequest request) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var respons = farskapsportalService.oppdatereFarskapserklaeringMedFarBorSammenInfo(fnrPaaloggetPerson, request);
    return new ResponseEntity<>(respons, HttpStatus.CREATED);
  }

  @GetMapping("/farskapserklaering/{idFarskapserklaering}/dokument")
  @Operation(description = "Henter dokument for en farskapserklæring",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Dokument hentet uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke farskapserklæring eller dokument"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<byte[]> henteDokument(@PathVariable int idFarskapserklaering) {
    var fnrPaaloggetPerson = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    var respons = farskapsportalService.henteDokumentinnhold(fnrPaaloggetPerson, idFarskapserklaering);
    return new ResponseEntity<>(respons, HttpStatus.OK);
  }
}
