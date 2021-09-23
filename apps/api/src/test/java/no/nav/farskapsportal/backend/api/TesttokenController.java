package no.nav.farskapsportal.backend.api;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.security.token.support.core.api.Unprotected;
import no.nav.security.token.support.test.jersey.TestTokenGeneratorResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/local")
@Unprotected
public class TesttokenController {

  @GetMapping("/generate-jwt/{foedselsnummer}")
  @Operation(description = "Henter testtoken for person")
  public String generereTesttoken(@PathVariable String foedselsnummer) {
    var tokengenerator = new TestTokenGeneratorResource();

    return tokengenerator.issueToken(foedselsnummer);
  }

}
