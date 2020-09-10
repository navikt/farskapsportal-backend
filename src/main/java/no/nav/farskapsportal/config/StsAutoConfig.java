package no.nav.farskapsportal.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.sts.TokenSupplier;
import no.nav.farskapsportal.consumer.sts.nav.NavSts;
import no.nav.farskapsportal.consumer.sts.nav.NavStsTokenSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestTemplate;

@Slf4j
@EnableAspectJAutoProxy
@ConditionalOnBean(RestTemplate.class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "pdl.libs.utils.common.sts.enabled")
public class StsAutoConfig {

    @Bean
    NavSts navSts(NavStsTokenSupplier supplier, @Value("${pdl.libs.utils.common.sts.refresh-on-seconds-left}") Long refreshSecondsLeft) {
        log.info("Token Supplier: Enabled, refreshing on {} seconds left of token. Using TokenSupplier {}", refreshSecondsLeft, supplier.getClass().getName());
        return new NavSts(supplier, refreshSecondsLeft);
    }

    @Bean
    @ConditionalOnMissingBean(TokenSupplier.class)
    NavStsTokenSupplier navStsTokenSupplier(
            RestTemplate restTemplate,
            @Value("${pdl.libs.utils.common.sts.nav.url}") String url,
            @Value("${pdl.libs.utils.common.sts.nav.username}") String username,
            @Value("${pdl.libs.utils.common.sts.nav.password}") String password) {
        return new NavStsTokenSupplier(restTemplate, url, username, password);
    }
}