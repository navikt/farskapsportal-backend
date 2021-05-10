package no.nav.farskapsportal.persistence.entity;


import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.service.PersonopplysningService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("StatusKontrollereFar")
@SpringBootTest(classes = {StatusKontrollereFar.class, Mapper.class, ModelMapper.class, PersonopplysningService.class})
@ActiveProfiles(PROFILE_TEST)
public class StatusKontrollereFarTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);

  @MockBean
  private PersonopplysningService personopplysningService;

  @Autowired
  private Mapper mapper;

  @Test
  @DisplayName("To objekter med samme mor, antall feilede forsøk, og tidspunkt for siste feilede forsøk skal gi samme hashkode")
  void toObjekterMedSammeMorAntallFeiledeForsoekOgTidspunktForSisteForsoekSkalGiSammeHashkode() {

    // given
    var tidspunktForNullstilling = LocalDateTime.now().plusHours(8);
    var antallFeiledeForsoek = 2;
    var objekt1 = StatusKontrollereFar.builder().mor(mapper.toEntity(MOR)).tidspunktForNullstilling(tidspunktForNullstilling)
        .antallFeiledeForsoek(antallFeiledeForsoek).build();
    var objekt2 = StatusKontrollereFar.builder().mor(mapper.toEntity(MOR)).tidspunktForNullstilling(tidspunktForNullstilling)
        .antallFeiledeForsoek(antallFeiledeForsoek).build();

    // then
    assertThat(objekt1.hashCode()).isEqualTo(objekt2.hashCode());
  }

  @Test
  @DisplayName("Skal IKKE gi samme hashkode for To objekter med samme mor, antall feilede forsøk, men forskjellige tidspunkt for siste feilede forsøk")
  void skalIkkeGiSammeHashkodeForToObjekterMedSammeMorAntallFeiledeForsoekMenForskjelligeTidspunktForSisteForsoek() {

    // given
    var tidspunktForNullstilling = LocalDateTime.now().plusHours(5);
    var antallFeiledeForsoek = 2;
    var objekt1 = StatusKontrollereFar.builder().mor(mapper.toEntity(MOR)).tidspunktForNullstilling(tidspunktForNullstilling)
        .antallFeiledeForsoek(antallFeiledeForsoek).build();
    var objekt2 = StatusKontrollereFar.builder().mor(mapper.toEntity(MOR)).tidspunktForNullstilling(tidspunktForNullstilling.plusHours(1))
        .antallFeiledeForsoek(antallFeiledeForsoek).build();

    // then
    assertThat(objekt1.hashCode()).isNotEqualTo(objekt2.hashCode());
  }


}
