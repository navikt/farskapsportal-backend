package no.nav.farskapsportal.backend.libs.dto.joark.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OpprettJournalpostRequest {

  private JournalpostType journalpostType;

  private AvsenderMottaker avsenderMottaker;

  private Bruker bruker;

  private String tema;

  private String behandlingstema;

  private String tittel;

  private String kanal;

  private String journalfoerendeEnhet;

  private String eksternReferanseId;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private Date datoMottatt;

  private List<Tilleggsopplysning> tilleggsopplysninger = new ArrayList<>();

  private Sak sak;

  @Builder.Default
  @NotNull(message = "dokumenter kan ikke være null")
  private List<Dokument> dokumenter = new ArrayList<>();

}
