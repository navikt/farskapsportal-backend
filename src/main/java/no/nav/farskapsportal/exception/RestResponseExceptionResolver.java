package no.nav.farskapsportal.exception;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.farskapsportal.api.FarskapserklaeringFeilResponse;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.consumer.esignering.ESigneringFeilException;
import no.nav.farskapsportal.consumer.pdl.PdlApiErrorException;
import no.nav.farskapsportal.consumer.pdl.RessursIkkeFunnetException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class RestResponseExceptionResolver {

  private final ExceptionLogger exceptionLogger;

  @ResponseBody
  @ExceptionHandler(IllegalArgumentException.class)
  protected ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = e.getMessage() == null || e.getMessage().isBlank() ? "Restkall feilet!" : e.getMessage();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseEntity<>(feilmelding, headers, HttpStatus.BAD_REQUEST));
  }

  @ResponseBody
  @ExceptionHandler(ValideringException.class)
  protected ResponseEntity<?> handleValideringException(ValideringException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    return generereFeilrespons("Validering av innleste verdier feilet!", e.getFeilkode(), HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ExceptionHandler(PdlApiErrorException.class)
  protected ResponseEntity<?> handlePdlApiException(PdlApiErrorException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Feil oppstod i kommunikasjon med PDL!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ResponseBody
  @ExceptionHandler(ESigneringFeilException.class)
  protected ResponseEntity<?> handleEsigneringFeilException(ESigneringFeilException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Feil oppstod i kommunikasjon med PDL!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), HttpStatus.INTERNAL_SERVER_ERROR);

  }

  @ResponseBody
  @ExceptionHandler(FeilIDatagrunnlagException.class)
  protected ResponseEntity<?> handleFeilIDatagrunnlagException(FeilIDatagrunnlagException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Feil oppstod i kommunikasjon med PDL!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ResponseBody
  @ExceptionHandler(RessursIkkeFunnetException.class)
  protected ResponseEntity<?> handleRessursIkkeFunnetException(RessursIkkeFunnetException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    return generereFeilrespons("Oppgitt ressurs ble ikke funnet!", e.getFeilkode(), HttpStatus.NOT_FOUND);
  }

  @ResponseBody
  @ExceptionHandler({EksisterendeFarskapserklaeringException.class, MorHarIngenNyfoedteUtenFarException.class, ManglerRelasjonException.class,
      OppretteFarskapserklaeringException.class})
  protected ResponseEntity<?> handleOppretteFarskapExceptions(OppretteFarskapserklaeringException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Opprettelse av farskapserklæring feilet!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ExceptionHandler({OppretteSigneringsjobbException.class})
  protected ResponseEntity<?> handleOppretteSigneringsjobbException(OppretteSigneringsjobbException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Opprettelse av esigneringsjobb hos Posten feilet!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<?> generereFeilrespons(String feilmelding, Feilkode feilkode, HttpStatus httpStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    var respons = FarskapserklaeringFeilResponse.builder().feilkode(feilkode).feilkodebeskrivelse(feilkode.getBeskrivelse()).build();

    return ResponseEntity.status(httpStatus).body(new ResponseEntity<>(respons, headers, httpStatus));
  }
}
