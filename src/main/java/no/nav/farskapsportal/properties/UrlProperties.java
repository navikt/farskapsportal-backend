package no.nav.farskapsportal.properties;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties(prefix = "farskapsportal-api.urls")
public class UrlProperties {
    @NotNull
    @NestedConfigurationProperty
    private PdlApiUrls pdlApi;

    @NotNull
    @NestedConfigurationProperty
    private NavStsUrls navSts;

    @Getter
    @ConstructorBinding
    @AllArgsConstructor
    public static class PdlApiUrls {
        private String baseUrl;
        private String graphql;
        private String identhendelser;
    }

    @Getter
    @ConstructorBinding
    @AllArgsConstructor
    public static class NavStsUrls {
        private String baseUrl;
        private String jwt;
    }
    
}
