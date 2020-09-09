package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@ApiModel
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BekrefteFarskapRequest {
    private String farsNavn;
    private String farsFodselsnummer;
    private LocalDate termindato;

}
