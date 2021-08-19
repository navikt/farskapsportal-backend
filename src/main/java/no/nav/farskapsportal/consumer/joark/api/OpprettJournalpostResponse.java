package no.nav.farskapsportal.consumer.joark.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OpprettJournalpostResponse {

    private String journalpostId;
    private String journalstatus;
    private String melding;
    private Boolean journalpostferdigstilt;
    private List<DokumentInfo> dokumenter;
}
