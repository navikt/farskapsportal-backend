package no.nav.farskapsportal.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.pdl.PdlApiHelsesjekkConsumer;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farskapsportal/helsesjekk")
@Unprotected
@Slf4j
public class FarskapsportalHelsesjekkController {

  @Autowired private PdlApiHelsesjekkConsumer pdlApiHelsesjekkConsumer;

  @RequestMapping(value = "/pdlApiIsAlive", method = RequestMethod.OPTIONS)
  public ResponseEntity<Boolean> pdlApiIsAlive() {
    return pdlApiHelsesjekkConsumer.pdlApiGraphqlErILive().getResponseEntity();
  }
}
