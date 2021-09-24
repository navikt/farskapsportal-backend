package no.nav.farskapsportal.backend.libs.felles.gcp.secretmanager;

import lombok.Value;

@Value
public class FarskapKeystoreCredentials {

  String alias;
  String password;
  String type;
}
