# ns-framework-planner-docker

Este modulo implementa el puerto de planificacion ejecutando un planner dentro de Docker.

## Para que sirve

- Construir el problema PDDL de entrada.
- Invocar un contenedor Docker con el planner.
- Parsear el plan generado y adaptarlo al modelo del core.

## Que contiene

- `src/main/java/com/tesis/nsframework/planner/docker/DockerPlanner.java`: adaptador principal hacia Docker.
- `src/main/java/com/tesis/nsframework/planner/docker/PddlProblemWriter.java`: escritura de archivos de problema PDDL.
- `src/main/java/com/tesis/nsframework/planner/docker/PlanTextParser.java`: parseo del plan textual a acciones planificadas.

## Dependencias principales

- `ns-framework-core`
- `slf4j-api`

