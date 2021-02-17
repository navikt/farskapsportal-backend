package no.nav.farskapsportal;

import io.swagger.annotations.ApiOperation;
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
  @ApiOperation("Henter testtoken for person")
  public String generereTesttoken(@PathVariable String foedselsnummer) {
    var tokengenerator = new TestTokenGeneratorResource();

    return tokengenerator.issueToken(foedselsnummer);
  }

}
