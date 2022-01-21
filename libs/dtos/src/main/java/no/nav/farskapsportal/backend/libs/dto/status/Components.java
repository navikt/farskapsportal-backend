package no.nav.farskapsportal.backend.libs.dto.status;

import no.nav.farskapsportal.backend.libs.dto.status.components.Db;
import no.nav.farskapsportal.backend.libs.dto.status.components.DiskSpace;
import no.nav.farskapsportal.backend.libs.dto.status.components.Statuskomponent;

public class Components {

  Db db;
  DiskSpace diskSpace;
  Statuskomponent livenessState;
  Statuskomponent ping;
  Statuskomponent readinessState;
  Statuskomponent refreshScope;
}
