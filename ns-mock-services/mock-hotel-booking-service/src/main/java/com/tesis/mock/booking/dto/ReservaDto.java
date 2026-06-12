package com.tesis.mock.booking.dto;

import java.time.LocalDate;

public record ReservaDto(String id, String usuarioId, String habitacionId, String hotelId, EstadoReserva estado, LocalDate fecha) {
}

