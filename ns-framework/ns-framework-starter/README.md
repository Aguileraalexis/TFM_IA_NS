# ns-framework-starter

Este modulo ofrece una dependencia unica para incorporar los modulos principales del framework.

## Para que sirve

- Simplificar el consumo del framework en proyectos cliente.
- Evitar declarar cada modulo (`core`, `config`, `planner`, `llm`, `executor`) por separado.

## Que contiene

- `pom.xml` del modulo `jar` que agrega dependencias hacia los modulos funcionales.
- Clase marcador en `src/main/java` para publicar un JAR no vacio.

## Nota

El starter incluye `ns-framework-planner-docker`; por lo tanto, el proyecto consumidor debe contemplar la disponibilidad de Docker cuando use ese adaptador.

