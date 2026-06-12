package com.tesis.mock.tourism;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mock Tourist Attractions Service")
                        .version("0.1.0")
                        .description("Servicio mock para atractivos turísticos. " +
                                "Expone endpoints para consultar ciudades y los atractivos turísticos disponibles en cada una."));
    }
}

