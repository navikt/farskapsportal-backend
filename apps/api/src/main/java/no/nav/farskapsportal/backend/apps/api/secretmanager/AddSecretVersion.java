package no.nav.farskapsportal.backend.apps.api.secretmanager;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersion;
import com.google.protobuf.ByteString;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class AddSecretVersion {

  // Add a new version to the existing secret.
  public void addSecretVersion(String projectId, String secretId, byte[] secretData) throws IOException {
    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      SecretName secretName = SecretName.of(projectId, secretId);

      // Create the secret payload.
      SecretPayload payload =
          SecretPayload.newBuilder()
              .setData(ByteString.copyFrom(secretData))
              .build();

      // Add the secret version.
      SecretVersion version = client.addSecretVersion(secretName, payload);
      System.out.printf("Added secret version %s\n", version.getName());
    }
  }
}
