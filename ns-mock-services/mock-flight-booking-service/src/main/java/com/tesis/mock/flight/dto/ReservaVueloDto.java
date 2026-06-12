package com.tesis.mock.flight.dto;

import java.time.LocalDate;

public record ReservaVueloDto(
        String id,
        String usuarioId,
        String vueloId,
        String aerolineaId,
        String ciudadOrigenId,
        String ciudadDestinoId,
        LocalDate fecha,
        EstadoReservaVuelo estado
) {
}

