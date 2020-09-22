package no.nav.farskapsportal.consumer.pdl.api;

import lombok.*;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;

import java.util.List;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonDto {

    @Singular("kjoenn")
    List<KjoennDto> kjoenn;

}