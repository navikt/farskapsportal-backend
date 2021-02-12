package no.nav.farskapsportal.gcp.secretmanager;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersion;
import com.google.protobuf.ByteString;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Base64;

public class AddSecretVersion {

  public static void main(String[] args) throws IOException {

    var sercretId = "selfsigned-jceks";
    var projectId = "farskapsportal-dev-169c";

    var filnavn = "trust.jceks";

    try (InputStream inputStream = AddSecretVersion.class.getClassLoader().getResourceAsStream(filnavn)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke " + filnavn);

      } else {
        //addSecretVersion(projectId, sercretId, inputStream.readAllBytes());
        BufferedWriter writer = new BufferedWriter(new FileWriter("trust-b64.txt"));
        // Base64 encode
        String secretDataBase64 = Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        writer.write(secretDataBase64);
        writer.close();
      }
    }
  }

  // Add a new version to the existing secret.
  private static void addSecretVersion(String projectId, String secretId, byte[] secretData) throws IOException {
    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      SecretName secretName = SecretName.of(projectId, secretId);

      // Base64 encode
      String secretDataBase64 = Base64.getEncoder().encodeToString(secretData);

      // Create the secret payload.
      SecretPayload payload = SecretPayload.newBuilder().setData(ByteString.copyFrom(secretDataBase64, Charset.defaultCharset())).build();

      // Add the secret version.
      SecretVersion version = client.addSecretVersion(secretName, payload);
      System.out.printf("Added secret version %s\n", version.getName());
    }
  }
}
