package no.nav.farskapsportal.backend.apps.api.consumer.pdl;

import static no.nav.bidrag.generer.testdata.person.PersonidentGeneratorKt.genererFødselsnummer;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class PdlApiConsumerCachingTest {

  private static final String FNR_OPPGITT_FAR = genererFødselsnummer(null, null);
  private static final NavnDto REGISTRERT_NAVN_FAR =
      NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
  private static final String FNR_OPPGITT_MOR = genererFødselsnummer(null, null);
  private static final NavnDto REGISTRERT_NAVN_MOR =
      NavnDto.builder().fornavn("Kari").etternavn("Nordmann").build();

  private PdlApiConsumer mock;

  @Autowired private PdlApiConsumer pdlApiConsumer;

  @BeforeEach
  void setUp() {
    mock = AopTestUtils.getTargetObject(pdlApiConsumer);

    reset(mock);

    when(mock.hentNavnTilPerson(eq(FNR_OPPGITT_FAR))).thenReturn(REGISTRERT_NAVN_FAR);

    when(mock.hentNavnTilPerson(eq(FNR_OPPGITT_MOR)))
        .thenReturn(REGISTRERT_NAVN_MOR)
        .thenThrow(new RuntimeException("Navn should be cached!"));
  }

  @Test
  void foersteKallHenterDataFraPdl_PaafoelgendeKallHenterFraCache() {
    assertEquals(
        REGISTRERT_NAVN_FAR.getFornavn(),
        pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_FAR).getFornavn());
    verify(mock).hentNavnTilPerson(FNR_OPPGITT_FAR);

    assertEquals(
        REGISTRERT_NAVN_FAR.getFornavn(),
        pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_FAR).getFornavn());
    assertEquals(
        REGISTRERT_NAVN_FAR.getFornavn(),
        pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_FAR).getFornavn());
    verifyNoMoreInteractions(mock);

    assertEquals(
        REGISTRERT_NAVN_MOR.getFornavn(),
        pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_MOR).getFornavn());
    verify(mock).hentNavnTilPerson(FNR_OPPGITT_MOR);

    assertEquals(
        REGISTRERT_NAVN_MOR.getFornavn(),
        pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_MOR).getFornavn());
    assertEquals(
        REGISTRERT_NAVN_MOR.getFornavn(),
        pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_MOR).getFornavn());
    verifyNoMoreInteractions(mock);
  }

  @EnableCaching
  @Configuration
  @Profile("caching")
  public static class CachingTestConfig {

    @Bean
    public PdlApiConsumer pdlApiConsumerMock() {
      return mock(PdlApiConsumer.class);
    }

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("navn");
    }
  }
}
