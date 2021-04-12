package no.nav.farskapsportal.exception;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.farskapsportal.api.FarskapserklaeringFeilResponse;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.consumer.pdl.PdlApiErrorException;
import no.nav.farskapsportal.dto.StatusKontrollereFarDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    return generereFeilrespons("Validering av innleste verdier feilet!", e.getFeilkode(), Optional.empty(), HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ExceptionHandler(FeilNavnOppgittException.class)
  protected ResponseEntity<?> handleFeilNavnOppgittException(FeilNavnOppgittException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    return generereFeilrespons("Oppgitt navn på far stemmer ikke med registrert navn i Folkeregisteret", e.getFeilkode(),
        e.getStatusKontrollereFarDto(), HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ExceptionHandler({MappingException.class, EsigneringConsumerException.class})
  protected ResponseEntity<?> handleInternFeilException(UnrecoverableException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, "En intern feil har oppstått!");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseEntity<>(e.getMessage(), headers, HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @ResponseBody
  @ExceptionHandler(PdlApiErrorException.class)
  protected ResponseEntity<?> handlePdlApiException(PdlApiErrorException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Feil oppstod i kommunikasjon med PDL!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), Optional.empty(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ResponseBody
  @ExceptionHandler(FeilIDatagrunnlagException.class)
  protected ResponseEntity<?> handleFeilIDatagrunnlagException(FeilIDatagrunnlagException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Feil i datagrunnlag";

    return generereFeilrespons(feilmelding, e.getFeilkode(), Optional.empty(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ResponseBody
  @ExceptionHandler(RessursIkkeFunnetException.class)
  protected ResponseEntity<?> handleRessursIkkeFunnetException(RessursIkkeFunnetException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    return generereFeilrespons("Oppgitt ressurs ble ikke funnet!", e.getFeilkode(), Optional.empty(), HttpStatus.NOT_FOUND);
  }

  @ResponseBody
  @ExceptionHandler({MorHarIngenNyfoedteUtenFarException.class, ManglerRelasjonException.class, OppretteFarskapserklaeringException.class})
  protected ResponseEntity<?> handleOppretteFarskapExceptions(OppretteFarskapserklaeringException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Opprettelse av farskapserklæring feilet!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), Optional.empty(), HttpStatus.BAD_REQUEST);
  }

  @ResponseBody
  @ExceptionHandler({OppretteSigneringsjobbException.class})
  protected ResponseEntity<?> handleOppretteSigneringsjobbException(OppretteSigneringsjobbException e) {
    exceptionLogger.logException(e, "RestResponseExceptionResolver");

    var feilmelding = "Opprettelse av esigneringsjobb hos Posten feilet!";

    return generereFeilrespons(feilmelding, e.getFeilkode(), Optional.empty(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ResponseBody
  @ExceptionHandler(SkattConsumerException.class)
  protected  ResponseEntity<?> handleSkattConsumerException(SkattConsumerException e){
    exceptionLogger.logException(e.getOriginalException(), "RestResponseExceptionResolver");

    e.printStackTrace();

    var feilmelding = "Feil oppstod i konsument mot Skatt";

    return generereFeilrespons(feilmelding, e.getFeilkode(), Optional.empty(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<FarskapserklaeringFeilResponse> generereFeilrespons(String feilmelding, Feilkode feilkode,
      Optional<StatusKontrollereFarDto> statusKontrollereFarDto, HttpStatus httpStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.WARNING, feilmelding);

    var respons =
        statusKontrollereFarDto.isEmpty() ? FarskapserklaeringFeilResponse.builder().feilkode(feilkode).feilkodebeskrivelse(feilkode.getBeskrivelse())
            .build() : FarskapserklaeringFeilResponse.builder().feilkode(feilkode)
            .antallResterendeForsoek(Optional.of(statusKontrollereFarDto.get().getAntallResterendeForsoek()))
            .feilkodebeskrivelse(feilkode.getBeskrivelse()).build();

    return new ResponseEntity<>(respons, headers, httpStatus);
  }
}
