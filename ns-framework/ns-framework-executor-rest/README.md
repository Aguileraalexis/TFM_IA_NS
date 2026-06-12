# ns-framework-executor-rest

Este modulo implementa la ejecucion de acciones del plan contra APIs REST.

## Para que sirve

- Resolver el binding de una accion simbolica a una llamada HTTP concreta.
- Construir requests con plantillas y datos del estado.
- Evaluar condiciones de exito y reportar resultados al orquestador.

## Que contiene

- `src/main/java/com/tesis/nsframework/executor/rest/RestActionExecutor.java`: ejecutor REST principal.
- `src/main/java/com/tesis/nsframework/executor/rest/TemplateInterpolator.java`: interpolacion de parametros en plantillas.
- `src/main/java/com/tesis/nsframework/executor/rest/SuccessConditionEvaluator.java`: evaluacion de exito en respuestas HTTP.

## Dependencias principales

- `ns-framework-core`
- `ns-framework-config`
- `jackson-databind`
- `slf4j-api`

