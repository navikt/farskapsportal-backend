package no.nav.farskapsportal.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.consumer.sts.SecurityTokenServiceConsumer;
import no.nav.farskapsportal.service.FarskapsportalService;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static no.nav.farskapsportal.consumer.sts.SecurityTokenServiceEndpointName.HENTE_IDTOKEN_FOR_SERVICEUSER;

@Slf4j
@Configuration
public class FarskapsportalApiConfig {

    public static final String ISSUER = "issuer";
    public static final String X_API_KEY = "X-Nav-ApiKey";

    @Bean
    public ConsumerEndpoint consumerEndpoint() {
        return new ConsumerEndpoint();
    }

    @Bean
    SecurityTokenServiceConsumer securityTokenServiceConsumer(
            @Qualifier("sts") RestTemplate restTemplate,
            @Value("${urls.apigw}") String baseUrl,
            @Value("${urls.sts.security-token-service-endpoint}") String endpoint,
            ConsumerEndpoint consumerEndpoint) {
        log.info("Oppretter SecurityTokenServiceConsumer med url {}", baseUrl);
        consumerEndpoint.addEndpoint(HENTE_IDTOKEN_FOR_SERVICEUSER, endpoint);
        restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
        return new SecurityTokenServiceConsumer(restTemplate, consumerEndpoint);
    }

    @Bean
    public PdlApiConsumer pdlApiConsumer(
            @Qualifier("pdl-api") RestTemplate restTemplate,
            @Value("${urls.apigw}") String baseUrl,
            @Value("${urls.pdl-api.graphql-endpoint}") String pdlApiEndpoint,
            ConsumerEndpoint consumerEndpoint) {
        consumerEndpoint.addEndpoint(PdlApiConsumerEndpointName.PDL_API_GRAPHQL, pdlApiEndpoint);
        restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
        log.info("Oppretter PdlApiConsumer med url {}", baseUrl);
        return PdlApiConsumer.builder().restTemplate(restTemplate).pdlApiGraphqlEndpoint(consumerEndpoint).build();
    }

    @Bean
    public FarskapsportalService farskapsportalService(
            PdlApiConsumer pdlApiConsumer
    ) {
        return FarskapsportalService.builder()
                .pdlApiConsumer(pdlApiConsumer)
                .build();
    }

    @Bean
    public OidcTokenManager oidcTokenManager(TokenValidationContextHolder tokenValidationContextHolder) {
        return () -> Optional.ofNullable(tokenValidationContextHolder)
                .map(TokenValidationContextHolder::getTokenValidationContext)
                .map(tokenValidationContext -> tokenValidationContext.getJwtTokenAsOptional(ISSUER))
                .map(Optional::get)
                .map(JwtToken::getTokenAsString)
                .orElseThrow(() -> new IllegalStateException("Kunne ikke videresende Bearer token"));
    }

    @FunctionalInterface
    public interface OidcTokenManager {
        String hentIdToken();
    }

}
