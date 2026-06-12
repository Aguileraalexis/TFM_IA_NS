# ns-framework-demo-app

Standalone Spring Boot demo application for the travel use case over `ns-framework`.

## 1. Resolver dependencias del framework

Si no tienes publicados los artefactos en GitHub Packages, puedes instalar el framework local del workspace:

```bash
cd ../ns-framework
mvn install -DskipTests
```

Si prefieres GitHub Packages, usa `settings-github-packages.xml` como referencia.

## 2. Levantar mocks

```bash
cd ../ns-mock-services
docker compose up --build
```

## 3. Ejecutar la app demo

```bash
mvn spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## 4. Endpoints principales

### Ejecutar viaje

```http
POST /api/execute
Content-Type: application/json
```

Example payload:

```json
{
  "input": "Quiero viajar desde Madrid para visitar Torre Eiffel y Museo del Louvre el 2026-07-10"
}
```

### Ver el `domain.pddl` dinamico

```http
GET /api/domain
```

Ese endpoint genera el dominio al vuelo consultando los mocks via OpenFeign.

## 5. Notas tecnicas

- El catalogo de ciudades, vuelos, hoteles y atractivos se consulta en tiempo de ejecucion via OpenFeign.
- El `domain.pddl` se genera dinamicamente con `TravelDomainPddlGenerator`; incluye una accion PDDL por cada ruta de vuelo disponible en el mock.
- La construccion del `PlanningProblem` tambien usa datos vivos de mocks (rutas, hoteles, atractivos).
- El planner simbolico es `DockerPlanner` (Fast Downward via `aibasel/downward`). Requiere Docker disponible en el host.
- La ejecucion de acciones de vuelo/hotel usa OpenFeign hacia los mocks correspondientes.
- Los argumentos del plan PDDL son procesados en mayusculas (IDs de ciudad/hotel/atractivo) para coincidir con los IDs de los servicios mock.

## 6. Imagen Docker del planner

```bash
docker pull aibasel/downward
```

La imagen se descargara automaticamente la primera vez que se invoque `POST /api/execute`.
