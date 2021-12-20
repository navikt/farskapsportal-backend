package no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain;

import no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.components.Db;
import no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.components.DiskSpace;
import no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.components.Statuskomponent;

public class Components {

  Db db;
  DiskSpace diskSpace;
  Statuskomponent livenessState;
  Statuskomponent ping;
  Statuskomponent readinessState;
  Statuskomponent refreshScope;
}
