package no.nav.farskapsportal.backend.libs.felles.consumer.pdl;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
public class PdlApiConsumerCachingTest {

  private final static String FNR_OPPGITT_FAR = "01018512345";
  private final static NavnDto REGISTRERT_NAVN_FAR = NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
  private final static String FNR_OPPGITT_MOR = "01018512245";
  private final static NavnDto REGISTRERT_NAVN_MOR = NavnDto.builder().fornavn("Kari").etternavn("Nordmann").build();

  private PdlApiConsumer mock;

  @Autowired
  private PdlApiConsumer pdlApiConsumer;

  @BeforeEach
  void setUp() {
    mock = AopTestUtils.getTargetObject(pdlApiConsumer);

    reset(mock);
    
    when(mock.hentNavnTilPerson(eq(FNR_OPPGITT_FAR)))
        .thenReturn(REGISTRERT_NAVN_FAR);

    when(mock.hentNavnTilPerson(eq(FNR_OPPGITT_MOR)))
        .thenReturn(REGISTRERT_NAVN_MOR)
        .thenThrow(new RuntimeException("Navn should be cached!"));
  }

  @Test
  void foersteKallHenterDataFraPdl_PaafoelgendeKallHenterFraCache() {
    assertEquals(REGISTRERT_NAVN_FAR.getFornavn(), pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_FAR).getFornavn());
    verify(mock).hentNavnTilPerson(FNR_OPPGITT_FAR);

    assertEquals(REGISTRERT_NAVN_FAR.getFornavn(), pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_FAR).getFornavn());
    assertEquals(REGISTRERT_NAVN_FAR.getFornavn(), pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_FAR).getFornavn());
    verifyNoMoreInteractions(mock);

    assertEquals(REGISTRERT_NAVN_MOR.getFornavn(), pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_MOR).getFornavn());
    verify(mock).hentNavnTilPerson(FNR_OPPGITT_MOR);

    assertEquals(REGISTRERT_NAVN_MOR.getFornavn(), pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_MOR).getFornavn());
    assertEquals(REGISTRERT_NAVN_MOR.getFornavn(), pdlApiConsumer.hentNavnTilPerson(FNR_OPPGITT_MOR).getFornavn());
    verifyNoMoreInteractions(mock);
  }

  @EnableCaching
  @Configuration
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
