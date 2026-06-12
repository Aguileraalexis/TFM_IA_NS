# mock-hotel-booking-service

Servicio mock de reservas de hotel por ciudad y fecha.

## Regla principal
- El cliente reserva por `hotelId` y `fecha`.
- El servicio asigna automaticamente la primera habitacion libre del hotel para esa fecha.
- Si no hay habitaciones libres, responde `409 CONFLICT`.

## Endpoints
- `GET /ciudades`
- `GET /hoteles`
  - filtro opcional: `GET /hoteles?ciudadId=MADR`
- `POST /reservas`
- `GET /reservas`
- `GET /reservas/{reservaId}`

## Ejemplo de reserva
```json
{
  "usuarioId": "1",
  "hotelId": "HT018",
  "fecha": "2026-06-20"
}
```

## Ejemplo de respuesta
```json
{
  "id": "RS-1A2B3C4D",
  "usuarioId": "1",
  "habitacionId": "RM0052",
  "hotelId": "HT018",
  "estado": "BOOKED",
  "fecha": "2026-06-20"
}
```

