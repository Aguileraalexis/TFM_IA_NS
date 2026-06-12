package com.tesis.nsdemo.config;

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
                        .title("NS Framework Demo — Travel Planner")
                        .version("0.1.0-SNAPSHOT")
                        .description("API del demo de planificación de viajes basada en el NS Framework. " +
                                "Recibe un objetivo en lenguaje natural, genera un plan PDDL y ejecuta las acciones " +
                                "contra los servicios de hotel, vuelos y turismo."));
    }
}

