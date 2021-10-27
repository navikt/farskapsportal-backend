package no.nav.farskapsportal.backend.apps.api.secretmanager;

import lombok.Value;

@Value
public class FarskapKeystoreCredentials {

  String alias;
  String password;
  String type;
}
