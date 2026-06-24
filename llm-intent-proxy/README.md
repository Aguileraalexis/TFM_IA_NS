# LLM Intent Mini Proxy (OpenAI/Anthropic)

Small FastAPI proxy that exposes a single endpoint compatible with `HttpLlmIntentInterpreter`:

- `POST /interpret` with JSON containing `prompt`
- Returns `{ "content": "<json-string>" }`

The inner JSON string must contain:

```json
{
  "intent": "plan_trip",
  "entities": {
    "originCityId": "MADR",
    "targetCityIds": "LOGR",
    "requestedAttractionIds": "AT021",
    "travelDate": "2026-07-10"
  },
  "constraints": {},
  "confidence": 0.93
}
```

## 1) Install

```powershell
cd C:\Users\aguil\OneDrive\Documentos\Cursos\AI-UNIR\TFE\demo\todo\llm-intent-proxy
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 2) Configure provider

### OpenAI

```powershell
$env:PROXY_PROVIDER = "openai"
$env:OPENAI_API_KEY = "<your_openai_key>"
$env:OPENAI_MODEL = "gpt-4o-mini"
```

### Anthropic

```powershell
$env:PROXY_PROVIDER = "anthropic"
$env:ANTHROPIC_API_KEY = "<your_anthropic_key>"
$env:ANTHROPIC_MODEL = "claude-3-5-haiku-latest"
```

Optional:

```powershell
$env:PROXY_TIMEOUT_SECONDS = "60"
```

## 3) Run proxy

```powershell
uvicorn app:app --host 0.0.0.0 --port 8090
```

## 4) Test proxy directly

```powershell
curl.exe -s -X POST http://localhost:8090/interpret -H "Content-Type: application/json" -d "{\"prompt\":\"Extract intent from: Quiero viajar desde Madrid para visitar Universidad de La Rioja el 2026-07-10\"}"
```

## 5) Connect `ns-framework-demo`

```powershell
$env:DEMO_TRAVEL_INTERPRETER_TYPE = "http-llm"
$env:DEMO_TRAVEL_LLM_ENDPOINT = "http://localhost:8090/interpret"
$env:DEMO_TRAVEL_LLM_TIMEOUT = "60s"
```

Start demo app:

```powershell
cd C:\Users\aguil\OneDrive\Documentos\Cursos\AI-UNIR\TFE\demo\todo\ns-framework-demo
mvn spring-boot:run
```

## Request options supported

Extra fields sent by `HttpLlmIntentInterpreter` under `request.*` are passed to the proxy and used when relevant:

- `model`
- `temperature`
- `max_tokens`
- `system`
- `provider` (override per-request)
- `catalog-base-url`
- `catalog-cities-path`
- `catalog-attractions-path`

## Notes

- This proxy targets `OpenAI /v1/chat/completions` and `Anthropic /v1/messages`.
- It enforces JSON output and returns it in `content` so the existing Java parser can consume it.
- The travel catalog context is rendered at runtime using `{{CITY_CATALOG}}` and `{{ATTRACTION_CATALOG}}` placeholders before the upstream LLM call.
- By default the catalog is fetched from `http://localhost:8085/ciudades` and `http://localhost:8085/atractivos`, but you can override those values per request or via environment variables.

