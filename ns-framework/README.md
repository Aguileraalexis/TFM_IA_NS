# NS Framework

Proyecto inicial multi-módulo Maven para un framework neuro-simbólico que combina:

- interpretación de lenguaje natural mediante un LLM
- planificación simbólica mediante PDDL y un planificador externo
- ejecución de acciones contra APIs REST
- sincronización de estado y replanificación

## Módulos

- `ns-framework-core`: interfaces, modelos de dominio, orquestador y servicios predeterminados
- `ns-framework-config`: cargadores de configuración externa para enlaces de acción a API
- `ns-framework-planner-docker`: adaptador que invoca un planificador dentro de Docker
- `ns-framework-llm-http`: intérprete de intención LLM genérico basado en HTTP
- `ns-framework-executor-rest`: ejecutor REST que mapea acciones del plan a llamadas API

