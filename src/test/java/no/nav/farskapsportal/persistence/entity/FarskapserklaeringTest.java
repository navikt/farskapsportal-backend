package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Farskapserklaering")
@SpringBootTest(classes = Farskapserklaering.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapserklaeringTest {

  @Test
  @DisplayName("Skal gi ulike hashkoder dersom to farskapserklæringer ikke gjelder samme parter")
  void skalGiUlikeHashkoderDersomToFarskapserklaeringerIkkeGjelderSammeParter() throws URISyntaxException {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far = Forelder.builder().foedselsnummer("01018832145").fornavn("Roger").etternavn("Mer").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var etDokument = Dokument.builder().dokumentnavn("signertErklaeringMor.pdf").dokumentStatusUrl("").redirectUrlFar(redirectUrlFar)
        .redirectUrlMor(redirectUrlMor).padesUrl("").build();

    var farskapserklaering = Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(etDokument).build();

    var etAnnetBarn = Barn.builder().termindato(barn.getTermindato()).build();
    var enAnnenMor = Forelder.builder().foedselsnummer("31019123450").fornavn("Greta").etternavn("Xyz").build();
    var etAnnetDokument = Dokument.builder().padesUrl("").redirectUrlMor(redirectUrlMor).redirectUrlFar(redirectUrlFar).dokumentStatusUrl("")
        .dokumentnavn("EtAnnetDokument.pdf").build();

    var enAnnenFarskapserklaering = Farskapserklaering.builder().barn(etAnnetBarn).mor(enAnnenMor).far(far).dokument(etAnnetDokument).build();

    // when, then
    assertNotEquals(farskapserklaering.hashCode(), enAnnenFarskapserklaering.hashCode());
  }

  @Test
  @DisplayName("Skal gi like hashkoder dersom to farskapserklæringer gjelder samme parter")
  void skalGiLikeHashkoderDersomToFarskapserklaeringerGjelderSammeParter() throws URISyntaxException {
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far = Forelder.builder().foedselsnummer("01038832140").fornavn("Roger").etternavn("Mer").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument = Dokument.builder().padesUrl("").redirectUrlMor(redirectUrlMor).redirectUrlFar(redirectUrlFar).dokumentStatusUrl("")
        .dokumentnavn("farskapserklaering.pdf").build();

    var farskapserklaering = Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    var sammeMor = Forelder.builder().foedselsnummer(mor.getFoedselsnummer()).fornavn(mor.getFornavn()).etternavn(mor.getEtternavn()).build();
    var sammeFar = Forelder.builder().foedselsnummer(far.getFoedselsnummer()).fornavn(far.getFornavn()).etternavn(far.getEtternavn()).build();

    var enAnnenFarskapserklaeringMedSammeParter = Farskapserklaering.builder().barn(barn).mor(sammeMor).far(sammeFar).dokument(dokument).build();

    // when, then
    assertEquals(farskapserklaering.hashCode(), enAnnenFarskapserklaeringMedSammeParter.hashCode());
  }

  @Test
  @DisplayName("To farskapserklæringer skal ikke kategoriseres som like dersom partene ikke er de samme")
  void farskapserklaeringerMedUlikeParterSkalKategoriseresSomUlike() throws URISyntaxException {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far = Forelder.builder().foedselsnummer("01018832145").fornavn("Roger").etternavn("Mer").build();
    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument = Dokument.builder().padesUrl("").redirectUrlMor(redirectUrlMor).redirectUrlFar(redirectUrlFar).dokumentStatusUrl("")
        .dokumentnavn("farskapserklaering.pdf").build();

    var farskapserklaering = Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    var etAnnetBarn = Barn.builder().termindato(barn.getTermindato()).build();
    var enAnnenMor = Forelder.builder().foedselsnummer("31019123450").fornavn("Greta").etternavn("Xyz").build();

    var enAnnenFarskapserklaering = Farskapserklaering.builder().barn(etAnnetBarn).mor(enAnnenMor).far(far).dokument(dokument).build();

    // when, then
    assertNotEquals(farskapserklaering, enAnnenFarskapserklaering);
  }

  @Test
  @DisplayName("To farskapserklæringer skal kategoriseres som like dersom alle parter er like")
  void farskapserklaeringerMedLikeParterSkalKategoriseresSomLike() throws URISyntaxException {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far = Forelder.builder().foedselsnummer("01038832140").fornavn("Roger").etternavn("Mer").build();

    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument = Dokument.builder().padesUrl("").redirectUrlMor(redirectUrlMor).redirectUrlFar(redirectUrlFar).dokumentStatusUrl("")
        .dokumentnavn("farskapserklaering.pdf").build();

    var farskapserklaering = Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    mor.setErklaeringerMor(Set.of(farskapserklaering));
    far.setErklaeringerFar(Set.of(farskapserklaering));

    var sammeMor = Forelder.builder().foedselsnummer(mor.getFoedselsnummer()).fornavn(mor.getFornavn()).etternavn(mor.getEtternavn()).build();

    var sammeFar = Forelder.builder().foedselsnummer(far.getFoedselsnummer()).fornavn(far.getFornavn()).etternavn(far.getEtternavn()).build();

    var enAnnenFarskapserklaeringMedSammeParter = Farskapserklaering.builder().barn(barn).mor(sammeMor).far(sammeFar).dokument(dokument).build();

    sammeMor.setErklaeringerMor(Set.of(enAnnenFarskapserklaeringMedSammeParter));
    sammeFar.setErklaeringerFar(Set.of(enAnnenFarskapserklaeringMedSammeParter));

    // when, then
    assertEquals(farskapserklaering, enAnnenFarskapserklaeringMedSammeParter);
  }

  @Test
  @DisplayName("Teste toString")
  void testeToString() throws URISyntaxException {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor = Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far = Forelder.builder().foedselsnummer("01038832140").fornavn("Roger").etternavn("Mer").build();
    var redirectUrlMor = "https://redirect-mor";
    var redirectUrlFar = "https://redirect-far";

    var dokument = Dokument.builder().padesUrl("").redirectUrlMor(redirectUrlMor).redirectUrlFar(redirectUrlFar).dokumentStatusUrl("")
        .dokumentnavn("farskapserklaering.pdf").build();

    var farskapserklaering = Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

    mor.setErklaeringerMor(Set.of(farskapserklaering));
    far.setErklaeringerFar(Set.of(farskapserklaering));

    // when
    var toString = farskapserklaering.toString();

    // then
    assertEquals(
        "Farskapserklaering gjelder " + barn.toString() + " med foreldrene: \n -Mor: " + mor.toString() + "\n -Far: "
            + far.toString(), toString);
  }
}
