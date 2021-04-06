package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplication.ISSUER;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.DokumentinnholdDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.persistence.dao.SigneringsinformasjonDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnExpression("${INTEGRATION_TEST_CONTROLLER_PAA:false}")
@RequestMapping("/dev/integration-test/management")
@ProtectedWithClaims(issuer = ISSUER)
@Slf4j
public class IntegrationTestManagementController {

  @Autowired
  private BarnDao barnDao;

  @Autowired
  private DokumentDao dokumentDao;

  @Autowired
  private DokumentinnholdDao dokumentinnholdDao;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private ForelderDao forelderDao;

  @Autowired
  private StatusKontrollereFarDao statusKontrollereFarDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  @Autowired
  private SigneringsinformasjonDao signeringsinformasjonDao;

  @GetMapping("/testdata/slette")
  @ApiOperation("Sletter lokale testdata. Tilgjengelig kun i DEV.")
  public String sletteTestdata() {

    try {
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      dokumentDao.deleteAll();
      dokumentinnholdDao.deleteAll();
      barnDao.deleteAll();
      signeringsinformasjonDao.deleteAll();
      meldingsloggDao.deleteAll();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return "Testdata slettet fra Farskapsportal public-skjema";
  }
}
