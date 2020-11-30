package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("FarskapserklaeringService")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapsportalServiceTest {

  @MockBean PdfGeneratorConsumer pdfGeneratorConsumer;
  @MockBean DifiESignaturConsumer difiESignaturConsumer;
  @MockBean PersistenceService persistenceService;
  @MockBean PersonopplysningService personopplysningService;

  @Autowired private FarskapsportalService farskapsportalService;

  @Nested
  @DisplayName("Teste henteBrukerinformasjon")
  class HenteBrukerinformasjon {

    @Test
    @DisplayName("Skal returnere påbegynte farskapserklæringer som venter på far")
    void skalReturnerePaabegynteFarskapserklaeringerSomVenterPaaFar() {
      // given
      var personnummerFar = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummerFar =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar;

      var far =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerFar)
              .fornavn("Ronald")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.FAR)
              .build();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          FarskapserklaeringDto.builder().far(far).build();

      when(personopplysningService.bestemmeForelderrolle(far.getFoedselsnummer()))
          .thenReturn(far.getForelderRolle());
      when(persistenceService.henteFarskapserklaeringerSomManglerMorsSignatur(
              far.getFoedselsnummer(), far.getForelderRolle()))
          .thenReturn(Set.of(farskapserklaeringSomVenterPaaFarsSignatur));
      when(persistenceService.henteFarskapserklaeringer(far.getFoedselsnummer()))
          .thenReturn(Set.of(farskapserklaeringSomVenterPaaFarsSignatur));

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(far.getFoedselsnummer());

      // then
      assertTrue(
          brukerinformasjon
              .getFarsVentendeFarskapserklaeringer()
              .containsAll(Set.of(farskapserklaeringSomVenterPaaFarsSignatur)));
    }
  }

  @Nested
  @DisplayName("Teste oppretteFarskapserklaering")
  class OppretteFarskapserklaering {

    @Test
    @DisplayName("test")
    void test() {
      assertTrue(false);
    }
  }

  @Nested
  @DisplayName("Teste henteSignertDokumentEtterRedirect")
  class HenteSignertDokumentEtterRedirect {
    @Test
    @DisplayName("test")
    void test() {
      assertTrue(false);
    }
  }
}
