package eu.h2020.symbiote;


import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static springfox.documentation.builders.PathSelectors.ant;

@EnableSwagger2
@Configuration
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SwaggerConfig.class);

    @Bean
    public Docket restApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
//                .securitySchemes(asList(
//                        new OAuth(
//                                "petstore_auth",
//                                asList(new AuthorizationScope("write_pets", "modify pets in your account"),
//                                        new AuthorizationScope("read_pets", "read your pets")),
//                                Arrays.<GrantType>asList(new ImplicitGrant(new LoginEndpoint("http://petstore.swagger.io/api/oauth/dialog"), "tokenName"))
//                        ),
//                        new ApiKey("api_key", "api_key", "header")
//                ))
                .produces(producesSet())
                .select()
                .paths(Predicates.and(ant("/**"), Predicates.not(ant("/error")), Predicates.not(ant("/management/**")), Predicates.not(ant("/management*"))))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Resource Access Proxy")
                .description("Resource Access Proxy API Description")
                .contact(new Contact("TestName", "http:/test-url.com", "test@test.de"))
                .license("BSD 3-Clause License")
                .licenseUrl("https://github.com/symbiote-h2020/ResourceAccessProxy/blob/master/LICENSE.txt")
                .version("0.2.0")
                .build();
    }

    private Set<String> producesSet() {
        Set<String> set = new HashSet<String>();
        set.add("application/json");

        return set;
    }
}
