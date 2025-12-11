package no.nav.farskapsportal.backend.libs.entity;

import static no.nav.bidrag.generer.testdata.person.PersonidentGeneratorKt.genererFødselsnummer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Forelder")
@SpringBootTest(classes = Forelder.class)
@ActiveProfiles("test")
public class ForelderTest {

  @Test
  @DisplayName("To objekter med samme fødselsnummer skal gi samme hashkode")
  void toObjekterMedSammeFoedselsnummerSkalGiSammeHashkode() {

    // given
    var etForeldreobjekt =
        Forelder.builder().foedselsnummer(genererFødselsnummer(null, null)).build();
    var etAnnetForeldreobjektForSammePerson =
        Forelder.builder().foedselsnummer(etForeldreobjekt.getFoedselsnummer()).build();

    // when, then
    assertEquals(etForeldreobjekt.hashCode(), etAnnetForeldreobjektForSammePerson.hashCode());
  }

  @Test
  @DisplayName("To objekter med forskjellige fødselsnummer skal forskjellig samme hashkode")
  void toObjekterMedUlikeFoedselsnummreSkalGiUlikeHashkoder() {

    // given
    var etForeldreobjekt =
        Forelder.builder().foedselsnummer(genererFødselsnummer(null, null)).build();

    var etAnnetForeldreobjektForEnAnnenPerson =
        Forelder.builder().foedselsnummer(genererFødselsnummer(null, null)).build();

    // when, then
    assertNotEquals(etForeldreobjekt.hashCode(), etAnnetForeldreobjektForEnAnnenPerson.hashCode());
  }

  @Test
  @DisplayName("To foreldreobjekter beskriver ikke samme person dersom de har ulike fødselsnummer")
  void toForeldreErUlikeDersomDeHarForskjelligeFoedselsnumre() {

    var enForelder = Forelder.builder().foedselsnummer(genererFødselsnummer(null, null)).build();

    var enAnnenForelder =
        Forelder.builder().foedselsnummer(genererFødselsnummer(null, null)).build();

    // when, then
    assertNotEquals(
        enForelder,
        enAnnenForelder,
        "Dersom to foreldreobjekter har forskjellig fødselsnummer, representerer de ikke samme person");
  }

  @Test
  @DisplayName("To foreldreobjekter beskriver samme person dersom de har like fødselsnummer")
  void toForeldreErLikeDersomDeHarSammeFoedselsnummer() {

    // given
    var enForelder = Forelder.builder().foedselsnummer(genererFødselsnummer(null, null)).build();

    var enAnnenForelder = Forelder.builder().foedselsnummer(enForelder.getFoedselsnummer()).build();

    // when, then
    assertEquals(
        enForelder,
        enAnnenForelder,
        "Dersom to foreldreinstanser har samme fødselsnummer, representerer de samme person");
  }
}
