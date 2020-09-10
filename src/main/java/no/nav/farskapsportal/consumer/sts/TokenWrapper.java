package no.nav.farskapsportal.consumer.sts;

import java.time.LocalDateTime;
import lombok.Value;

@Value
public class TokenWrapper {
    String token;
    LocalDateTime expiry;
}