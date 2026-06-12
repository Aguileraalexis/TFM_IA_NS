package com.tesis.nsdemo.client.dto;

import java.time.LocalDate;

public record HotelReservationDto(
        String id,
        String usuarioId,
        String habitacionId,
        String hotelId,
        String estado,
        LocalDate fecha
) {
}

