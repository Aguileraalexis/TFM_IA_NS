package com.tesis.nsdemo.client.dto;

import java.time.LocalDate;

public record HotelReservationRequest(String usuarioId, String hotelId, LocalDate fecha) {
}

