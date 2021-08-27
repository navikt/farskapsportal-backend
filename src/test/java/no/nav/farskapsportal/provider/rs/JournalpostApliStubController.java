package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_SKATT_SSL_TEST;
import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.joark.api.OpprettJournalpostRequest;
import no.nav.farskapsportal.consumer.joark.api.OpprettJournalpostResponse;
import no.nav.security.token.support.core.api.Unprotected;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Unprotected
@RequestMapping("/rest/journalpostapi/v1")
@Slf4j
@ActiveProfiles({PROFILE_TEST, PROFILE_SKATT_SSL_TEST})
public class JournalpostApliStubController {

  @PostMapping(value = "/journalpost")
  public ResponseEntity<OpprettJournalpostResponse> registrereFarskap(@RequestBody OpprettJournalpostRequest request) {
    var jpId = Integer.toString(LocalDateTime.now().getNano());

    Validate.isTrue(request.getSak() != null);

    return new ResponseEntity<>(OpprettJournalpostResponse.builder().journalpostId(jpId).build(), HttpStatus.CREATED);
  }
}
