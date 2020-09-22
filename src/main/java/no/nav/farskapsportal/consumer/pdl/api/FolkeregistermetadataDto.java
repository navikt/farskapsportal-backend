package no.nav.farskapsportal.consumer.pdl.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FolkeregistermetadataDto {
    LocalDateTime ajourholdstidspunkt;
    LocalDateTime gyldighetstidspunkt;
    LocalDateTime opphoerstidspunkt;
    String kilde;
    String aarsak;
    Integer sekvens;
    @JsonIgnore
    Boolean gjeldende;
}
