# mock-flight-booking-service

Servicio mock de reservas de vuelos entre ciudades.

## Regla principal
- Una reserva se crea con `usuarioId`, `aerolineaId`, `ciudadOrigenId`, `ciudadDestinoId` y `fecha`.
- Las rutas habilitadas se cargan desde `data/vuelos.json`.
- Cada vuelo tiene tope de 5 reservas por fecha.

## Endpoints
- `GET /ciudades`
- `GET /aerolineas`
- `GET /usuarios`
- `GET /vuelos`
- `POST /reservas-vuelos`
- `GET /reservas-vuelos`
- `GET /reservas-vuelos/{reservaId}`

## Ejemplo de reserva
```json
{
  "usuarioId": "1",
  "aerolineaId": "AL04",
  "ciudadOrigenId": "MADR",
  "ciudadDestinoId": "DUBA",
  "fecha": "2026-06-20"
}
```
