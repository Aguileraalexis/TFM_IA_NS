package com.tesis.mock.booking;

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
                        .title("Mock Hotel Booking Service")
                        .version("0.1.0")
                        .description("Servicio mock para la reserva de hoteles. " +
                                "Expone endpoints para consultar ciudades, hoteles y gestionar reservas de habitaciones."));
    }
}

