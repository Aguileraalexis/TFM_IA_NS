package com.tesis.nsdemo.client.dto;

import java.time.LocalDate;

public record FlightReservationDto(
        String id,
        String usuarioId,
        String vueloId,
        String aerolineaId,
        String ciudadOrigenId,
        String ciudadDestinoId,
        LocalDate fecha,
        String estado
) {
}

