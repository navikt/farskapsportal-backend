package no.nav.farskapsportal.backend.libs.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Farskapserklaering")
@SpringBootTest(classes = Farskapserklaering.class)
@ActiveProfiles("test")
public class FarskapserklaeringTest {

  @Test
  @DisplayName("Skal gi ulike hashkoder dersom to farskapserklæringer ikke gjelder samme parter")
  void skalGiUlikeHashkoderDersomToFarskapserklaeringerIkkeGjelderSammeParter() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01018832145").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var etDokument =
        Dokument.builder()
            .navn("signertErklaeringMor.pdf")
            .statusUrl("")
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .build();

    var farskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(etDokument).build();

    var etAnnetBarn = Barn.builder().termindato(barn.getTermindato()).build();
    var enAnnenMor = Forelder.builder().foedselsnummer("31019123450").build();
    var etAnnetDokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("EtAnnetDokument.pdf")
            .build();

    var enAnnenFarskapserklaering =
        Farskapserklaering.builder()
            .barn(etAnnetBarn)
            .mor(enAnnenMor)
            .far(far)
            .dokument(etAnnetDokument)
            .build();

    // when, then
    assertNotEquals(farskapserklaering.hashCode(), enAnnenFarskapserklaering.hashCode());
  }

  @Test
  @DisplayName("Skal gi like hashkoder dersom to farskapserklæringer gjelder samme parter")
  void skalGiLikeHashkoderDersomToFarskapserklaeringerGjelderSammeParter() {
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01038832140").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("farskapserklaering.pdf")
            .build();

    var farskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    var sammeMor = Forelder.builder().foedselsnummer(mor.getFoedselsnummer()).build();
    var sammeFar = Forelder.builder().foedselsnummer(far.getFoedselsnummer()).build();

    var enAnnenFarskapserklaeringMedSammeParter =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(sammeMor)
            .far(sammeFar)
            .dokument(dokument)
            .build();

    // when, then
    assertEquals(farskapserklaering.hashCode(), enAnnenFarskapserklaeringMedSammeParter.hashCode());
  }

  @Test
  @DisplayName(
      "To farskapserklæringer skal ikke kategoriseres som like dersom partene ikke er de samme")
  void farskapserklaeringerMedUlikeParterSkalKategoriseresSomUlike() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01018832145").build();
    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("farskapserklaering.pdf")
            .build();

    var farskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    var etAnnetBarn = Barn.builder().termindato(barn.getTermindato()).build();
    var enAnnenMor = Forelder.builder().foedselsnummer("31019123450").build();

    var enAnnenFarskapserklaering =
        Farskapserklaering.builder()
            .barn(etAnnetBarn)
            .mor(enAnnenMor)
            .far(far)
            .dokument(dokument)
            .build();

    // when, then
    assertNotEquals(farskapserklaering, enAnnenFarskapserklaering);
  }

  @Test
  @DisplayName("To farskapserklæringer skal kategoriseres som like dersom alle parter er like")
  void farskapserklaeringerMedLikeParterSkalKategoriseresSomLike() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01038832140").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("farskapserklaering.pdf")
            .build();

    var farskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    var sammeMor = Forelder.builder().foedselsnummer(mor.getFoedselsnummer()).build();

    var sammeFar = Forelder.builder().foedselsnummer(far.getFoedselsnummer()).build();

    var enAnnenFarskapserklaeringMedSammeParter =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(sammeMor)
            .far(sammeFar)
            .dokument(dokument)
            .build();

    // when, then
    assertEquals(farskapserklaering, enAnnenFarskapserklaeringMedSammeParter);
  }

  @Test
  void
      deaktivertFarskapserklaeringEllersIdentiskMedAktivFarskapserklaeringSkalKategoriseresSomUlike() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01038832140").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("farskapserklaering.pdf")
            .build();

    var aktivFarskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
    var deaktivFarskapserklaering =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(far)
            .dokument(dokument)
            .deaktivert(LocalDateTime.now())
            .build();

    // when, then
    assertThat(aktivFarskapserklaering).isNotEqualTo(deaktivFarskapserklaering);
  }

  @Test
  void
      toFarskapserklaeringerMedSammeParterMenForskjelligDeaktiveringstidspunktSkalKategoriseresSomUlike() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01038832140").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("farskapserklaering.pdf")
            .build();

    var deaktivFarskapserklaering =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(far)
            .dokument(dokument)
            .deaktivert(LocalDateTime.now().minusMinutes(3))
            .build();
    var deaktivFarskapserklaering2 =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(far)
            .dokument(dokument)
            .deaktivert(LocalDateTime.now())
            .build();

    // when, then
    assertThat(deaktivFarskapserklaering).isNotEqualTo(deaktivFarskapserklaering2);
  }

  @Test
  @DisplayName("Teste toString")
  void testeToString() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").build();
    var far = Forelder.builder().foedselsnummer("01038832140").build();
    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument =
        Dokument.builder()
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
            .statusUrl("")
            .navn("farskapserklaering.pdf")
            .build();

    var farskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    // when
    var toString = farskapserklaering.toString();

    // then
    assertEquals(
        "Farskapserklaering gjelder "
            + barn.toString()
            + " med foreldrene: \n -Mor: "
            + mor
            + "\n -Far: "
            + far,
        toString);
  }
}
