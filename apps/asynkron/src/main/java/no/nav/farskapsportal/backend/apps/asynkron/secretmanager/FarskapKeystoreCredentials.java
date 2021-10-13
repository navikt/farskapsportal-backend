package no.nav.farskapsportal.backend.apps.asynkron.secretmanager;

import lombok.Value;

@Value
public class FarskapKeystoreCredentials {

  String alias;
  String password;
  String type;
}
