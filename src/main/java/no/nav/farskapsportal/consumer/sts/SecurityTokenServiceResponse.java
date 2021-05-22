package no.nav.farskapsportal.consumer.sts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SecurityTokenServiceResponse {

  @JsonProperty("access_token")
  private String idToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("expires_in")
  private String expiresIn;
}
