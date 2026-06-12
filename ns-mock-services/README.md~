# ns-mock-services with Docker

## Services
- mock-user-service -> http://localhost:8081
- mock-booking-service -> http://localhost:8082
- mock-payment-service -> http://localhost:8083

## Run all services
```bash
docker compose up --build
```

## Stop
```bash
docker compose down
```

## Example requests

### Register user
```bash
curl -X POST http://localhost:8081/users/register \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1"}'
```

### List slots
```bash
curl http://localhost:8082/slots
```

### Create appointment
```bash
curl -X POST http://localhost:8082/appointments \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","slotId":"slot1"}'
```

### Create payment
```bash
curl -X POST http://localhost:8083/payments \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","amount":99.99,"mode":"SUCCESS"}'
```
