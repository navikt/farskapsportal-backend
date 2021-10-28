package no.nav.farskapsportal.backend.libs.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("StatusKontrollereFar")
@SpringBootTest(classes = StatusKontrollereFar.class)
@ActiveProfiles("test")
public class StatusKontrollereFarTest {

  private static final Forelder MOR = Forelder.builder().foedselsnummer("12345678910").build();

  @Test
  @DisplayName("To objekter med samme mor, antall feilede forsøk, og tidspunkt for siste feilede forsøk skal gi samme hashkode")
  void toObjekterMedSammeMorAntallFeiledeForsoekOgTidspunktForSisteForsoekSkalGiSammeHashkode() {

    // given
    var tidspunktForNullstilling = LocalDateTime.now().plusHours(8);
    var antallFeiledeForsoek = 2;
    var objekt1 = StatusKontrollereFar.builder().mor(MOR).tidspunktForNullstilling(tidspunktForNullstilling)
        .antallFeiledeForsoek(antallFeiledeForsoek).build();
    var objekt2 = StatusKontrollereFar.builder().mor(MOR).tidspunktForNullstilling(tidspunktForNullstilling)
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
    var objekt1 = StatusKontrollereFar.builder().mor(MOR)
        .tidspunktForNullstilling(tidspunktForNullstilling)
        .antallFeiledeForsoek(antallFeiledeForsoek).build();
    var objekt2 = StatusKontrollereFar.builder().mor(MOR).tidspunktForNullstilling(tidspunktForNullstilling.plusHours(1))
        .antallFeiledeForsoek(antallFeiledeForsoek).build();

    // then
    assertThat(objekt1.hashCode()).isNotEqualTo(objekt2.hashCode());
  }
}
