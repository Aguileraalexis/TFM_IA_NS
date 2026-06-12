# Plan de implementacion: demo de viajes con NS Framework

## Objetivo
Construir una demo en `ns-framework-demo` donde el usuario ingresa un prompt en lenguaje natural con:
- ciudad de origen
- lista de atractivos (y/o ciudades) a visitar

La app debe:
1. Inferir ciudades destino desde atractivos.
2. Generar un `problem.pddl` dinamico con rutas reales de vuelos.
3. Planificar con planner simbolico (PDDL).
4. Ejecutar acciones REST para reservar vuelos y hoteles en mocks.

## Diagnostico actual (verificado)
- `ns-framework-demo` hoy usa stubs (`DemoIntentInterpreter`, `DemoPlanner`, `DemoActionExecutor`) y dominio `demo-commerce`.
- En framework, el `problem.pddl` se genera dinamicamente desde `PlanningProblem` (`PddlProblemWriter`), pero el problema de base que llega al writer depende del `PlanningProblemBuilder`.
- `DefaultPlanningProblemBuilder` solo arma `objects` con parametros del goal, no con todos los simbolos requeridos por estado/rutas.
- El orquestador no refresca estado desde APIs antes de planear; solo usa `StateStore` + `StateUpdater` despues de ejecutar acciones.
- `RestActionExecutor` trabaja con `baseUrl` unico y `effectsMapping` estatico.
- En mocks:
  - `mock-flight-booking-service`: OK, expone `GET /vuelos` y `POST /reservas-vuelos`.
  - `mock-tourist-attractions-service`: OK, expone atractivos/ciudades.
  - `mock-hotel-booking-service`: incompleto (no hay controlador REST).
  - `docker-compose.yml` esta desalineado (referencia servicios no existentes: `mock-user-service`, `mock-booking-service`, `mock-payment-service`).
  - Hay inconsistencia de datos: en vuelos aparece `LARJ`, pero en catalogo de ciudades esta `LOGR`.

## Respuesta corta a tu duda
Si, la generacion de `problem.pddl` puede ser dinamica en el framework actual, pero **no ocurre automaticamente con datos de mocks**. Para que use rutas reales de vuelos, debes construir dinamicamente el `PlanningProblem` (estado/objetos/facts) en la demo, consultando `GET /vuelos` antes de planificar.

## Plan por fases

## Fase 1 - Corregir plataforma mock (bloqueante)
- [x] Corregir `ns-mock-services/docker-compose.yml` para incluir solo servicios existentes y puertos reales.
- [x] Implementar `HotelBookingController` en `mock-hotel-booking-service` con endpoints minimos:
  - `GET /ciudades`
  - `GET /hoteles?ciudadId=`
  - `POST /reservas`
  - `GET /reservas` y `GET /reservas/{id}`
- [x] Alinear catalogos de ciudades/codigos entre flight, hotel y tourism (`LOGR` vs `LARJ`, `MADR` vs `MEDR`).
- [x] Agregar README del servicio de hoteles con contrato REST.

Entregable: stack mock levantable y coherente con datos compartidos.

## Fase 2 - Redefinir dominio PDDL de viajes
- [x] Generar `domain.pddl` dinamicamente con OpenFeign a mocks (`ciudades`, `vuelos`, `hoteles`, `atractivos`).
- [x] Exponer `GET /api/domain` para inspeccionar el dominio generado en runtime.
- [x] Definir dominio de viajes dinamico con:
  - tipos: `traveler`, `city`, `hotel`, `attraction`
  - predicados: `flight-available`, `at`, `visited-city`, `attraction-in-city`, `visited-attraction`, `hotel-in-city`, `hotel-booked`
  - acciones: `book-flight`, `book-hotel`, `visit-attraction`
- [x] Definir meta compatible con multipunto:
  - visitar ciudades derivadas de atractivos solicitados
  - reservar hotel en cada ciudad destino
- [x] Usar OpenFeign tambien para ejecutar reservas de vuelos y hoteles desde la demo.

Entregable: dominio PDDL listo para planner externo.

## Fase 3 - Construccion dinamica de problema (pieza central)
- [x] `TravelPlanningProblemBuilder` ya implementado desde Fase 2 y mejorado en esta fase:
  - consulta tourist-attractions para mapear atractivos -> ciudades
  - consulta flight-service para rutas (`GET /vuelos`)
  - arma `objects` con tipado PDDL: `traveler_1 - traveler`
  - arma `initFacts` con `(flight-available ORIG DEST)` real
  - arma `goalExpression` con ciudades, hoteles y atractivos requeridos
- [x] `StateStore` inicializado con estado vacio (estado inicial viene de `initFacts` del problema)
- [x] `problemName` con timestamp para trazabilidad

Entregable: `problem.pddl` dinamico por request, basado en datos vivos del mock.

## Fase 4 - Migrar demo a componentes reales del framework
- [x] Sustituir `TravelDemoPlanner` por `DockerPlanner` como bean Planner en `DemoBeansConfig`
- [x] `DockerPlanner` configurado via `application.yml` (imagen, comando, timeout)
- [x] Modificar `DockerPlanner` en framework para soportar planner que escribe plan-file directamente
- [x] `TravelFeignActionExecutor` reescrito para leer de `ExecutionContext` + `action.arguments()` (compatible con el plan parseado por `DockerPlanner`)
- [x] `TravelExecutionOrchestrator` enriquece el contexto con metadata del problema antes de ejecutar
- [x] `DomainMetadata` configurado con el dominio de viajes dinamico

Entregable: flujo completo NL -> goal -> domain dinamico -> problem dinamico -> DockerPlanner -> REST.

## Fase 5 - Ejecucion REST multi-servicio
- [x] Bindings de vuelo y hotel implementados directamente en `TravelFeignActionExecutor` via OpenFeign.
- [x] Limitacion de `baseUrl` unico resuelta: cada accion usa su Feign client dedicado.
- [x] Efectos simbolicos del estado mapeados en el executor:
  - `(flight-booked traveler from to)`, `(at traveler city)`, `(visited-city city)`
  - `(hotel-booked traveler hotel city)`, `(visited-attraction attraction)`

Entregable: reservas reales en mocks por cada paso del plan.

## Fase 6 - Validacion E2E
- [x] Tests unitarios: `TravelDomainPddlGeneratorTest`, `TravelPlanningProblemBuilderTest`, `TravelDemoPlannerTest`, `TravelFeignActionExecutorTest`
- [ ] Test de integracion end-to-end con mocks y Docker levantados.
- [ ] Caso negativo: destino sin ruta de vuelo.

Entregable: evidencia reproducible de comportamiento correcto.

## Cambios recomendados (framework y mocks)

## Minimos para avanzar
- Mocks: SI, cambios obligatorios (hotel controller + compose + datos consistentes).
- Framework: NO obligatorio si implementas builder y adaptadores custom en demo.

## Recomendados en framework (mejoras)
- `DefaultPlanningProblemBuilder`: opcion para incluir objetos inferidos desde `initFacts`/catalogos.
- `RestActionExecutor`: soportar `baseUrl` por accion (o endpoint absoluto).
- `DefaultExecutionOrchestrator`: hook opcional `StateRefresher` pre-plan para sincronizar estado desde APIs.
- Efectos dinamicos: permitir plantillas en `effectsMapping` usando response/context.

## Riesgos
- Desalineacion de IDs entre catalogos rompe carga y planificacion.
- Planner puede fallar si faltan objetos tipados en problema.
- Prompt ambiguo (atractivo no encontrado) requiere estrategia de desambiguacion.

## Orden sugerido de implementacion
1. Fix mocks (compose + hotel + datos).
2. Nuevo dominio PDDL.
3. `TravelPlanningProblemBuilder` dinamico.
4. Integracion `DockerPlanner` + bindings REST.
5. Pruebas E2E y endurecimiento.


