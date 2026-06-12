package com.tesis.mock.flight;

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
                        .title("Mock Flight Booking Service")
                        .version("0.1.0")
                        .description("Servicio mock para la reserva de vuelos. " +
                                "Expone endpoints para consultar ciudades, aerolíneas, vuelos y gestionar reservas de vuelos."));
    }
}

