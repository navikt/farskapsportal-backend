package no.nav.farskapsportal.backend.libs.felles.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
public class CachingConfig {

  @Bean
  @Primary
  public CacheManager cacheManager() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES));
    return caffeineCacheManager;
  }

  @Bean
  public CacheManager cacheManagerSts() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(45, TimeUnit.MINUTES));
    return caffeineCacheManager;
  }
}
