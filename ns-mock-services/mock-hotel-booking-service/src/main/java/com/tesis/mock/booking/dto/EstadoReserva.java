package com.tesis.mock.booking.dto;

public enum EstadoReserva {
    BOOKED("Reservado"),
    CANCELLED("Cancelado"),
    COMPLETED("Completado");

    private final String descripcion;

    EstadoReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

