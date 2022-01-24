package no.nav.farskapsportal.backend.libs.felles.consumer.sts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

@ContextConfiguration
@ActiveProfiles("caching")
@ExtendWith(SpringExtension.class)
public class SecurityTokenServiceConsumerCachingTest {

  private final static String STS_TOKEN = "eaYaglakjag-agakglgag";

  private SecurityTokenServiceConsumer mock;

  @Autowired
  private SecurityTokenServiceConsumer securityTokenServiceConsumer;

  @BeforeEach
  void setUp() {

    mock = AopTestUtils.getTargetObject(securityTokenServiceConsumer);
    reset(mock);
    when(mock.hentIdTokenForServicebruker(anyString(), anyString()))
        .thenReturn(STS_TOKEN);
    when(mock.hentIdTokenForServicebruker(anyString(), anyString()))
        .thenReturn(STS_TOKEN)
        .thenThrow(new RuntimeException("STS-token should be cached!"));
  }

  @Test
  void foersteKallHenterTokenFraSTS_PaafoelgendeKallHenterFraCache() {
    var brukernavn = "srvfarskapsportal";
    var passord = "passord";

    assertEquals(STS_TOKEN, securityTokenServiceConsumer.hentIdTokenForServicebruker(brukernavn, passord));
    verify(mock).hentIdTokenForServicebruker(brukernavn, passord);

    assertEquals(STS_TOKEN, securityTokenServiceConsumer.hentIdTokenForServicebruker(brukernavn, passord));
    assertEquals(STS_TOKEN, securityTokenServiceConsumer.hentIdTokenForServicebruker(brukernavn, passord));
    verifyNoMoreInteractions(mock);
  }

  @EnableCaching
  @Configuration
  @Profile("caching")
  public static class CachingTestConfig {

    @Bean
    public SecurityTokenServiceConsumer securityTokenServiceConsumerMock() {
      return mock(SecurityTokenServiceConsumer.class);
    }

    @Bean("cacheManagerSts")
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("sts");
    }
  }
}
