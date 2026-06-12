# Ejemplos de uso - API de reservas de hoteles

## Flujo rapido

### 1. Obtener ciudades
```http
GET /ciudades
```

### 2. Obtener hoteles (opcionalmente por ciudad)
```http
GET /hoteles
GET /hoteles?ciudadId=MADR
```

### 3. Crear reserva
```http
POST /reservas
Content-Type: application/json
```

Request:
```json
{
  "usuarioId": "1",
  "hotelId": "HT018",
  "fecha": "2026-06-20"
}
```

Notas:
- No se solicita `habitacionId`.
- El servicio asigna automaticamente la primera habitacion libre en ese hotel para la fecha.
- Si no hay disponibilidad, responde `409 CONFLICT`.

Respuesta esperada (201):
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

### 4. Consultar reserva por ID
```http
GET /reservas/{reservaId}
```

### 5. Listar reservas
```http
GET /reservas
```


