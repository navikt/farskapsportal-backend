package no.nav.farskapsportal.backend.libs.dto.oppgave;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OppretteOppgaveForespoersel {

  public static final String OPPGAVETYPE_VURDER_DOKUMENT = "VUR";
  public static final String ENHETSNUMMER_SYSTEM = "9999";
  public static final String PRIORITET_HOEY = "HOY";
  public static final String ENHETSNUMMER_FARSKAP = "4833";
  public static final String TEMA_BIDRAG = "BID";

  String journalpostId;
  String oppgavetype = OPPGAVETYPE_VURDER_DOKUMENT;
  String opprettetAvEnhetsnr = ENHETSNUMMER_SYSTEM;
  String prioritet =  PRIORITET_HOEY;
  String tildeltEnhetsnr = ENHETSNUMMER_FARSKAP;
  String tema =  TEMA_BIDRAG;
  String aktivDato = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
  String saksreferanse  = null;
  String beskrivelse = null;
  String tilordnetRessurs= null;
  String fristFerdigstillelse = null;
}
