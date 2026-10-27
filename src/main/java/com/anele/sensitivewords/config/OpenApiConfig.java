package com.anele.sensitivewords.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
@Profile("local")
public class OpenApiConfig {
    @Bean
    OpenAPI sensitiveWordsOpenApi() {
        return new OpenAPI().info(new Info().title("Sensitive Words API").version("1.0"))
            .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("basicAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
