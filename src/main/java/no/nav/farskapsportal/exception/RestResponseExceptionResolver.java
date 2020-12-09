package no.nav.farskapsportal.exception;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.farskapsportal.consumer.esignering.ESigneringFeilException;
import no.nav.farskapsportal.consumer.pdl.PdlApiErrorException;
import no.nav.farskapsportal.consumer.pdl.PersonIkkeFunnetException;
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
  @ExceptionHandler(RestClientException.class)
  protected ResponseEntity<?> handleRestClientException(RestClientException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.SERVICE_UNAVAILABLE));
  }

  @ResponseBody
  @ExceptionHandler(IllegalArgumentException.class)
  protected ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.BAD_REQUEST));
  }

  @ResponseBody
  @ExceptionHandler(FeilForelderrollePaaOppgittPersonException.class)
  protected ResponseEntity<?> handleFeilKjoennPaaOppgittFarException(
      FeilForelderrollePaaOppgittPersonException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.BAD_REQUEST));
  }

  @ResponseBody
  @ExceptionHandler(OppgittNavnStemmerIkkeMedRegistrertNavnException.class)
  protected ResponseEntity<?> handleOppgittNavnStemmerIkkeMedRegistrertNavnException(
      OppgittNavnStemmerIkkeMedRegistrertNavnException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.BAD_REQUEST));
  }

  @ResponseBody
  @ExceptionHandler({PdlApiErrorException.class, ESigneringFeilException.class})
  protected ResponseEntity<?> handlePdlApiException(Exception e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @ResponseBody
  @ExceptionHandler(PersonIkkeFunnetException.class)
  protected ResponseEntity<?> handlePersonIkkeFunnetException(Exception e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.NOT_FOUND));

  }

  @ResponseBody
  @ExceptionHandler(FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException.class)
  protected  ResponseEntity<?> handleFarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException(Exception e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Restkall feilet!";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.BAD_REQUEST));
  }
}
