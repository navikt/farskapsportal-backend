package no.nav.farskapsportal.backend.libs.stubs.provider;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.joark.api.OpprettJournalpostRequest;
import no.nav.farskapsportal.backend.libs.dto.joark.api.OpprettJournalpostResponse;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rest/journalpostapi/v1")
public class JournalpostApliStubController {

  @PostMapping(value = "/journalpost")
  public ResponseEntity<OpprettJournalpostResponse> registrereFarskap(@RequestBody OpprettJournalpostRequest request) {
    var jpId = Integer.toString(LocalDateTime.now().getNano());

    Validate.isTrue(request.getSak() != null);

    return new ResponseEntity<>(OpprettJournalpostResponse.builder().journalpostId(jpId).build(), HttpStatus.CREATED);
  }
}
