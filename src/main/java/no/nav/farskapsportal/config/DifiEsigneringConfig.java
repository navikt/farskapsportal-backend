package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LOCAL;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.gcp.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.gcp.secretmanager.AddSecretVersion;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  @Value("${farskapsportal-api.disable-esignering}")
  private boolean disableEsignering;

  @Bean
  @Profile(PROFILE_LIVE)
  public KeyStoreConfig keyStoreConfig(
      @Value("${sm://projects/627047445397/secrets/virksomhetssertifikat-test-passord/versions/1}") String sertifikatP12Passord,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion) throws IOException {

    log.info("sert-pwd lengde: {}", sertifikatP12Passord.length());

    var projectId = "19909854975";
    var secretName = "test-virksomhetssertifikat-felles-keystore-jceks_2018-2021";
    var secretVersion = "1";
    var secretPayload = accessSecretVersion.accessSecretVersion(projectId, secretName, secretVersion);
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig.fromJavaKeyStore(inputStream, "nav-gcp", sertifikatP12Passord, sertifikatP12Passord);
  }

  @Bean
  @Profile({PROFILE_LIVE, PROFILE_LOCAL})
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(ServiceUri.DIFI_TEST).build();
  }

  @Bean
  @Profile(PROFILE_LOCAL)
  public KeyStoreConfig testKeyStoreConfig() throws IOException {

    var filnavn = "trust.jceks";
    var filInnholdB64 = "zs7OzgAAAAIAAAABAAAAAgANbmF2LWdjcC1qY2VrcwAAAXeQ7sTmAAVYLjUwOQAAA7QwggOwMIICmAIJAKWklzx2prVvMA0GCSqGSIb3DQEBCwUAMIGZMQswCQYDVQQGEwJOTzERMA8GA1UECAwIUm9nYWxhbmQxEjAQBgNVBAcMCVN0YXZhbmdlcjEMMAoGA1UECgwDTkFWMQ8wDQYDVQQLDAZCaWRyYWcxHjAcBgNVBAMMFWZhcnNrYXBzcG9ydGFsLm5hdi5ubzEkMCIGCSqGSIb3DQEJARYVZmFyc2thcHNwb3J0YWxAbmF2Lm5vMB4XDTIxMDIxMTExMzUxMloXDTMxMDIwOTExMzUxMlowgZkxCzAJBgNVBAYTAk5PMREwDwYDVQQIDAhSb2dhbGFuZDESMBAGA1UEBwwJU3RhdmFuZ2VyMQwwCgYDVQQKDANOQVYxDzANBgNVBAsMBkJpZHJhZzEeMBwGA1UEAwwVZmFyc2thcHNwb3J0YWwubmF2Lm5vMSQwIgYJKoZIhvcNAQkBFhVmYXJza2Fwc3BvcnRhbEBuYXYubm8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCWjkK6HyFz6ErWtcArEAR8s1kmiK+CsjRlF2Q/9t1RnpvIi6OtwSgKWJjha6PC9EBgz1YEzbQC8XOUHuoQ6SOY3DPjlMG9rOuypwYgvHySJjfXEQRR86GfYkj/TO9tYR4NVt9Z0DxubzNk5NiIcAm9Vtz3JLWY3bhQ1NJ29BgkrR1Fx5T0MU34M3nL/FCVNOqh9cz7KjmLOhtIOHoPKAeSx77GcnvTF0TarF1M7pKDd++HlTud5D98s5QcVO3TDR3Ei8goXyXZUWfg4KeQqz9b4UDaEE6xLibduEUuEd+PeXtJE/7wrJOlkO61xI/I6FZyAvnzPwEqBoxNgNl80OKJAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAEsrM8GLWiDL0X3xY9YBsC0DXCaItgxwfHxqALvyc1tBTnDublMqVBIcLweGKY5XqwYSsp9U0PF4dITDfsKt55HfXZS9Rvp48SJ0wudQ6wmqxzcAwNme9xlfVE1rVhBJP9zn3vtMXzAWs/71m/MHNMxF/oyo6JRsRH3e8IK19nCiy7A5LNIHh+F5XGO+BWULUns+1VcZbBXGTvjFmjGvazSqnEMt+qXpNUSlq8IAJLU++MxC7Mo/UVWsiYekp4UZlHLHfmTrIanL+KxEv8kXwreqEducdtboqqYkGEeadt/RoFkHwkQwZwcAXIx++2illvKudkTMFz2xyTnTfaMqfcXk1FUPZ+18CgK191PbFbCFnGvaFw==";
    filInnholdB64 = null;

    if (filInnholdB64 != null) {
      log.info("Henter innhold fra variabel");
      var decoded = new String(Base64.decodeBase64(filInnholdB64));
      var binaert = decoded.getBytes();
      return KeyStoreConfig.fromJavaKeyStore(new ByteArrayInputStream(binaert), "selfsigned", "safeone", "safeone");
    } else {
      log.info("Henter innhold fra fysisk fil");
      return readKeyStoreFromBinaryFileWithConversion(filnavn);
    }
  }

  private KeyStoreConfig readKeyStoreFromB64File(String filnavn) throws IOException {
    var classLoader = getClass().getClassLoader();
    try (InputStream inputStream = classLoader.getResourceAsStream(filnavn)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke " + filnavn);
      } else {
        var encodedString = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        var decoded = new String(Base64.decodeBase64(encodedString));
        return KeyStoreConfig.fromJavaKeyStore(new ByteArrayInputStream(decoded.getBytes()), "nav-gcp-jceks", "safeone", "safeone");
      }
    }
  }

  private KeyStoreConfig readKeyStoreFromBinaryFile(String filnavn) throws IOException {
    var classLoader = getClass().getClassLoader();
    try (InputStream inputStream = classLoader.getResourceAsStream(filnavn)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke " + filnavn);
      } else {
        return KeyStoreConfig.fromJavaKeyStore(inputStream, "nav-gcp-jceks", "safeone", "safeone");
      }
    }
  }

  private KeyStoreConfig readKeyStoreFromBinaryFileWithConversion(String filnavn) throws IOException {
    var classLoader = getClass().getClassLoader();
    try (InputStream inputStream = classLoader.getResourceAsStream(filnavn)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke " + filnavn);
      } else {
        var string = IOUtils.toString(inputStream, Charset.defaultCharset());
        var stringB64 = new String(Base64.encodeBase64(string.getBytes()));
        var decodedString = new String(Base64.decodeBase64(stringB64));
        var inputStreamTransformed = IOUtils.toInputStream(decodedString, Charset.defaultCharset());

        File file = new File("trust-converted.jceks");
        try (FileOutputStream fos = new FileOutputStream(file)) {
          fos.write(decodedString.getBytes());
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        return KeyStoreConfig.fromJavaKeyStore(inputStreamTransformed, "nav-gcp-jceks", "safeone", "safeone");
      }
    }
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(ClientConfiguration clientConfiguration, ModelMapper modelMapper, DirectClient directClient) {
    return new DifiESignaturConsumer(clientConfiguration, modelMapper, directClient, disableEsignering);
  }

}
