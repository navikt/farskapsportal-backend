package no.nav.farskapsportal.backend.libs.dto.oppgave;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Oppgaveforespoersel {

  public static final String BEHANDLINGSTYPE_NASJONAL = "ae0118";
  public static final String ENHETSNUMMER_FARSKAP = "4860";
  public static final String ENHETSNUMMER_SYSTEM = "9999";
  public static final String OPPGAVETYPE_GENERELL = "GEN";

  public static final String PRIORITET_NORMAL = "NORM";
  public static final String TEMA_BIDRAG = "BID";
  private static final String OPPGAVE_DATOFORMAT = "YYYY-MM-dd";

  private String aktivDato = LocalDate.now().format(DateTimeFormatter.ofPattern(OPPGAVE_DATOFORMAT));
  private String aktoerId;
  private String behandlingstype = BEHANDLINGSTYPE_NASJONAL;
  private String beskrivelse;
  private String fristFerdigstillelse = LocalDate.now().plusWeeks(1).format(DateTimeFormatter.ofPattern(OPPGAVE_DATOFORMAT));
  private String oppgavetype = OPPGAVETYPE_GENERELL;
  private String opprettetAvEnhetsnr = ENHETSNUMMER_SYSTEM;
  private String prioritet = PRIORITET_NORMAL;
  private String tema = TEMA_BIDRAG;
  private String tildeltEnhetsnr = ENHETSNUMMER_FARSKAP;
}
