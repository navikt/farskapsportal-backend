package no.nav.farskapsportal.config;

import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.consumer.sts.SecurityTokenServiceConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;

import static no.nav.farskapsportal.config.FarskapsportalApiConfig.X_API_KEY;

@Configuration
public class RestTemplateConfig {

    private static final String TEMA = "Tema";
    private static final String TEMA_FAR = "FAR";

    @Bean
    @Qualifier("base")
    @Scope("prototype")
    public HttpHeaderRestTemplate restTemplate() {
        HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();
        httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER,
                CorrelationIdFilter::fetchCorrelationIdForThread);
        return httpHeaderRestTemplate;
    }

    @Bean
    @Qualifier("sts")
    @Scope("prototype")
    public HttpHeaderRestTemplate stsRestTemplate(
            @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
            @Value("${apikey-sts-fp}") String xApiKeySts
    ) {
        httpHeaderRestTemplate.addHeaderGenerator(X_API_KEY, () -> xApiKeySts);
        return httpHeaderRestTemplate;
    }

    @Bean
    @Qualifier("pdl-api")
    @Scope("prototype")
    public HttpHeaderRestTemplate pdlApiRestTemplate(
            @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
            @Value("${farskapsportal-api.servicebruker.brukernavn}") String farskapsportalApiBrukernavn,
            @Value("${farskapsportal-api.servicebruker.passord}") String farskapsportalApiPassord,
            @Value("${apikey-pdlapi-fp}") String xApiKeyPdlApi,
            @Autowired SecurityTokenServiceConsumer securityTokenServiceConsumer) {
        httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION,
                () -> "Bearer " + securityTokenServiceConsumer
                        .hentIdTokenForServicebruker(farskapsportalApiBrukernavn, farskapsportalApiPassord));
        httpHeaderRestTemplate.addHeaderGenerator(TEMA, () -> TEMA_FAR);
        httpHeaderRestTemplate.addHeaderGenerator(X_API_KEY, () -> xApiKeyPdlApi);
        return httpHeaderRestTemplate;
    }

}
