package no.nav.farskapsportal.gcp.secretmanager;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(PROFILE_LIVE)
public class AccessSecretVersion {

  public SecretPayload accessSecretVersion() throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    var secretId = "selfsigned-jceks";
    var projectId = "farskapsportal-dev-169c";
    String versionId = "4";
    return accessSecretVersion(projectId, secretId, versionId);
  }

  // Access the payload for the given secret version if one exists. The version
  // can be a version number as a string (e.g. "5") or an alias (e.g. "latest").
  public SecretPayload accessSecretVersion(String projectId, String secretId, String versionId) throws IOException {
    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretId, versionId);

      // Access the secret version.
      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);

      // Print the secret payload.
      //
      // WARNING: Do not print the secret in a production environment - this
      // snippet is showing how to access the secret material.
      return response.getPayload();
    }
  }
}
