package no.nav.farskap.farskapsportalapi.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskap.farskapsportalapi.api.*;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static no.nav.farskap.farskapsportalapi.FarskapsportalApiApplication.ISSUER;

@RestController
@ProtectedWithClaims(issuer = ISSUER)
@Slf4j
public class FarskapsportalController {

    @PostMapping("/personopplysinger/kontroll")
    @ApiOperation("Kontrollerer om fødeslnummer stemmer med navn")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Oppgitt fødselsnummer stemmer med navn"),
            @ApiResponse(code = 400, message = "Ugyldig fødselsnummer, eller kombinasjon av fødselsnummer og navn"),
            @ApiResponse(code = 401, message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
            @ApiResponse(code = 503, message = "Kontroll av fødselsnummer mot navn feilet")
    })
    public ResponseEntity<KontrollerePersonopplysningerResponse> kontrollerePersonopplysninger(
            @RequestBody KontrollerePersonopplysningerRequest request
    ) {
        log.info("Starter kontroll av personopplysninger");
        var kontrollerePersonopplysningerResponse = new KontrollerePersonopplysningerResponse();

        log.info("Kontroll av personopplysninger fullført");
        return new ResponseEntity<>(kontrollerePersonopplysningerResponse, HttpStatus.OK);
    }

    @PostMapping("/farskap/bekreft")
    @ApiOperation("Bekrefter farskap til barn under svangerskap")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Farskap bekreftet"),
            @ApiResponse(code = 400, message = "Feil opplysinger angitt"),
            @ApiResponse(code = 401, message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
            @ApiResponse(code = 503, message = "Bekrefting av farskap feilet")
    })
    public ResponseEntity<BekrefteFarskapResponse> bekrefteFarskap(
            @RequestBody BekrefteFarskapRequest request
    ) {
        log.info("Starter kontroll av personopplysninger");
        var bekrefteFarskapResponse = new BekrefteFarskapResponse();

        log.info("Kontroll av personopplysninger fullført");
        return new ResponseEntity<>(bekrefteFarskapResponse, HttpStatus.OK);
    }

    @GetMapping("/far/{fodselsnummer}/termindato/{termindato}")
    @ApiOperation("Henter farskapserklæring under behandling som venter på fars signatur")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Farskapserklæring hentet"),
            @ApiResponse(code = 400, message = "Feil opplysinger angitt"),
            @ApiResponse(code = 401, message = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            @ApiResponse(code = 404, message = "Fant ikke fødselsnummer eller navn"),
            @ApiResponse(code = 503, message = "Bekrefting av farskap feilet")
    })
    public ResponseEntity<Farskapserklaring> henteFarskapserklaringUnderBehandling(@PathVariable String fodselsnummer, @PathVariable String termindato) {
        log.info("Hente farskapserklæring for barn med termindato {}", termindato);

        var farskapserklaring = new Farskapserklaring();

        return new ResponseEntity<>(farskapserklaring, HttpStatus.OK);
    }
}
