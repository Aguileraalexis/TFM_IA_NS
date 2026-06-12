# ns-framework-llm-http

Este modulo implementa la interpretacion de intenciones mediante llamadas HTTP a un servicio LLM.

## Para que sirve

- Recibir texto de usuario y contexto.
- Llamar a un endpoint HTTP de LLM.
- Transformar la respuesta en una estructura interpretable por el core.

## Que contiene

- `src/main/java/com/tesis/nsframework/llm/http/HttpLlmIntentInterpreter.java`: implementacion HTTP del puerto `IntentInterpreter`.

## Dependencias principales

- `ns-framework-core`
- `jackson-databind`
- `slf4j-api`

