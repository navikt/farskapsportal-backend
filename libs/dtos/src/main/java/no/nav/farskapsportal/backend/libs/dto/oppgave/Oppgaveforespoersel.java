package no.nav.farskapsportal.backend.libs.dto.oppgave;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Oppgaveforespoersel {

  public static final String OPPGAVETYPE_VURDER_DOKUMENT = "VUR";
  public static final String ENHETSNUMMER_SYSTEM = "9999";
  public static final String PRIORITET_HOEY = "HOY";
  public static final String ENHETSNUMMER_FARSKAP = "4833";
  public static final String TEMA_BIDRAG = "BID";

  private String oppgavetype = OPPGAVETYPE_VURDER_DOKUMENT;
  private String opprettetAvEnhetsnr = ENHETSNUMMER_SYSTEM;
  private String prioritet =  PRIORITET_HOEY;
  private String tildeltEnhetsnr = ENHETSNUMMER_FARSKAP;
  private String tema =  TEMA_BIDRAG;
  private String aktivDato = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
  private String saksreferanse  = null;
  private String beskrivelse = null;
  private String tilordnetRessurs= null;
  private String fristFerdigstillelse = null;
}
