package com.tesis.nsdemo.client.dto;

import java.time.LocalDate;

public record FlightReservationRequest(
        String usuarioId,
        String aerolineaId,
        String ciudadOrigenId,
        String ciudadDestinoId,
        LocalDate fecha
) {
}

