# ns-framework-core

Este modulo define el nucleo del framework neuro-simbolico.

## Para que sirve

- Modelar el dominio (objetivos, estado, plan, resultados).
- Definir puertos (interfaces) para planner, interprete LLM, ejecutor y almacenamiento de estado.
- Ofrecer implementaciones base para orquestacion y actualizacion de estado.

## Que contiene

- `src/main/java/com/tesis/nsframework/core/model/`: records y modelos (`GoalSpec`, `PlanningProblem`, `PlanResult`, `ExecutionResult`, etc.).
- `src/main/java/com/tesis/nsframework/core/port/`: contratos de integracion (`Planner`, `IntentInterpreter`, `ActionExecutor`, `StateStore`, etc.).
- `src/main/java/com/tesis/nsframework/core/service/`: servicios por defecto (`DefaultExecutionOrchestrator`, `DefaultGoalMapper`, `DefaultStateUpdater`, `InMemoryStateStore`).
- `src/main/java/com/tesis/nsframework/core/exception/`: excepciones del framework (`FrameworkException`).

## Dependencias principales

- `jackson-annotations`
- `slf4j-api`

