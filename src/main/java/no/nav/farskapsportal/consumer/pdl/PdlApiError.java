package no.nav.farskapsportal.consumer.pdl;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PdlApiError {
  private String message;
  private String code;
}
