package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import lombok.Value;

@ApiModel
@Value
public class Farskapserklaring {
  String termindato;
  String fodselsnummerBarn;
  String fodselsnummerMor;
  String fodselsnummerFar;
}
