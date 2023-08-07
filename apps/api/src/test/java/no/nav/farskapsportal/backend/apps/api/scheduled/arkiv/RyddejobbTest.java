package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_SCHEDULED_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Set;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.Arkiv;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("RyddejobbTest")
@ActiveProfiles(PROFILE_SCHEDULED_TEST)
@SpringBootTest(classes = {Ryddejobb.class, Arkiv.class, FarskapsportalAsynkronEgenskaper.class})
@AutoConfigureWireMock(port = 0)
public class RyddejobbTest {

  private @MockBean PersistenceService persistenceService;
  private @Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  private Ryddejobb ryddejobb;

  @BeforeEach
  void setup() {

    ryddejobb =
        Ryddejobb.builder()
            .persistenceService(persistenceService)
            .arkiv(farskapsportalAsynkronEgenskaper.getArkiv())
            .build();
  }

  @Test
  public void skalKjoereJobbForSlettingAvGamleDokumenter() {

    // given
    when(persistenceService.henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(any(), any()))
        .thenReturn(Set.of(1));

    // when
    ryddejobb.sletteGamleDokumenter();

    // then
    verify(persistenceService, times(2))
        .henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(any(), any());
    verify(persistenceService, times(1)).sletteDokumentinnhold(1);
  }

  @Test
  public void skalVerifisereAtGrenserErSattSomForventet() {

    // given
    var levetidDokumenter =
        farskapsportalAsynkronEgenskaper.getArkiv().getLevetidDokumenterIMaaneder();
    var maksAntallDokumenterPerKjoering =
        farskapsportalAsynkronEgenskaper.getArkiv().getMaksAntallDokumenterSomSlettesPerKjoering();

    // then
    assertThat(levetidDokumenter).isEqualTo(12);
    assertThat(maksAntallDokumenterPerKjoering).isEqualTo(1000);
  }
}
