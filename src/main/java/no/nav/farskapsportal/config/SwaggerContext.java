package no.nav.farskapsportal.config;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.ArrayList;
import java.util.List;
import no.nav.farskapsportal.FarskapsportalApiApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerContext {

  @Bean
  public Docket api() {

    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(
            RequestHandlerSelectors.basePackage(
                FarskapsportalApiApplication.class.getPackage().getName()))
        .paths(regex("/api.*"))
        .build()
        .securitySchemes(addSecuritySchemes())
        .securityContexts(List.of(securityContext()));
  }

  private List<SecurityScheme> addSecuritySchemes() {
    List<SecurityScheme> list = new ArrayList<>();
    list.add(apiKey());
    return list;
  }

  private ApiKey apiKey() {
    return new ApiKey("Bearer-token", HttpHeaders.AUTHORIZATION, "header");
  }

  private SecurityContext securityContext() {
    return SecurityContext.builder()
        .securityReferences(defaultAuth())
        .forPaths(PathSelectors.regex("/*.*"))
        .build();
  }

  private List<SecurityReference> defaultAuth() {
    List<SecurityReference> list = new ArrayList<>();
    AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
    AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
    authorizationScopes[0] = authorizationScope;

    return List.of(new SecurityReference("Bearer-token", authorizationScopes));
  }
}
