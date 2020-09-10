package no.nav.farskapsportal.config;

import no.nav.farskapsportal.properties.UrlProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
public class PropertiesConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "pdl.libs.utils.common.app.load-properties", matchIfMissing = true)
    @PropertySource(value = "common-app.yaml", factory = YamlPropertySourceFactory.class)
    public static class CommonAppConfig {
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "pdl.libs.utils.common.urls.load-properties", matchIfMissing = true)
    @PropertySource(value = "common-urls.yaml", factory = YamlPropertySourceFactory.class)
    @EnableConfigurationProperties(UrlProperties.class)
    public static class CommonUrlConfig {
    }
}