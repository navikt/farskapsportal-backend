package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Forelder")
@SpringBootTest(classes = Forelder.class)
@ActiveProfiles(PROFILE_TEST)
public class ForelderTest {

  @Test
  @DisplayName("To objekter med samme fødselsnummer skal gi samme hashkode")
  void toObjekterMedSammeFoedselsnummerSkalGiSammeHashkode() {

    // given
    var etForeldreobjekt =
        Forelder.builder()
            .foedselsnummer("01019232145")
            .fornavn("Bob")
            .etternavn("Builder")
            .build();
    var etAnnetForeldreobjektForSammePerson =
        Forelder.builder()
            .foedselsnummer(etForeldreobjekt.getFoedselsnummer())
            .fornavn(etForeldreobjekt.getFornavn())
            .etternavn(etForeldreobjekt.getEtternavn())
            .build();

    // when, then
    assertEquals(etForeldreobjekt.hashCode(), etAnnetForeldreobjektForSammePerson.hashCode());
  }

  @Test
  @DisplayName("To objekter med forskjellige fødselsnummer skal forskjellig samme hashkode")
  void toObjekterMedUlikeFoedselsnummreSkalGiUlikeHashkoder() {

    // given
    var etForeldreobjekt =
        Forelder.builder()
            .foedselsnummer("01019232145")
            .fornavn("Bob")
            .etternavn("Builder")
            .build();

    var etAnnetForeldreobjektForEnAnnenPerson =
        Forelder.builder()
            .foedselsnummer("01018945612")
            .fornavn("Onkel")
            .etternavn("Skrue")
            .build();

    // when, then
    assertNotEquals(etForeldreobjekt.hashCode(), etAnnetForeldreobjektForEnAnnenPerson.hashCode());
  }

  @Test
  @DisplayName("To foreldreobjekter beskriver ikke samme person dersom de har ulike fødselsnummer")
  void toForeldreErUlikeDersomDeHarForskjelligeFoedselsnumre() {

    var enForelder =
        Forelder.builder()
            .foedselsnummer("01015787654")
            .fornavn("Frank")
            .etternavn("Monsen")
            .build();

    var enAnnenForelder =
        Forelder.builder()
            .foedselsnummer("02025812345")
            .fornavn("Sverre")
            .etternavn("Rudberg")
            .build();

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
    var enForelder =
        Forelder.builder()
            .foedselsnummer("01015787654")
            .fornavn("Frank")
            .etternavn("Monsen")
            .build();

    var enAnnenForelder =
        Forelder.builder()
            .foedselsnummer(enForelder.getFoedselsnummer())
            .fornavn("Sverre")
            .etternavn("Rudberg")
            .build();

    // when, then
    assertEquals(
        enForelder,
        enAnnenForelder,
        "Dersom to foreldreinstanser har samme fødselsnummer, representerer de samme person");
  }

  @Test
  @DisplayName("Strengrepresentasjonen av foreldreobjektet skal inneholder fornavn og etternavn")
  void strengrepresentasjonenAvForeldreobjektetSkalInneholdeFornavnOgEtternavn() {

    // given
    var enForelder =
        Forelder.builder()
            .foedselsnummer("01015787654")
            .fornavn("Frank")
            .etternavn("Monsen")
            .build();

    var toString = enForelder.toString();

    // when, then
    assertEquals(
        toString, "Forelder: " + enForelder.getFornavn() + " " + enForelder.getEtternavn());
  }
}
