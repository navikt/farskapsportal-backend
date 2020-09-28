package no.nav.farskapsportal.service;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;

@Builder
@Slf4j
public class FarskapsportalService {

  private final PdlApiConsumer pdlApiConsumer;

  public HttpResponse<Kjoenn> henteKjoenn(String foedselsnummer) {
    return pdlApiConsumer.henteKjoenn(foedselsnummer);
  }

  public HttpResponse<Boolean> pdlApiIsAlive() {
    return pdlApiConsumer.pdlApiIsAlive();
  }
}
