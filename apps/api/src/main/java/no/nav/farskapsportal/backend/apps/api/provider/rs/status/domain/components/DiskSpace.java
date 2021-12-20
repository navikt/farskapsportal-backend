package no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain.components;

import lombok.Value;

@Value
public class DiskSpace extends Statuskomponent {

  DiskSpaceDetails details;

  @Value
  private class DiskSpaceDetails {
    String total;
    String free;
    String threshold;
    boolean exists;
  }
}
