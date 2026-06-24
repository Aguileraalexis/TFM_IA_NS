import json
import os
from typing import Any

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, ConfigDict


DEFAULT_SYSTEM_PROMPT = (
    "Return only valid JSON with fields: intent, entities, constraints, confidence. "
    "Do not add markdown fences."
)


class InterpretRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    prompt: str
    format: str | None = None


app = FastAPI(title="LLM Intent Proxy", version="0.1.0")


def _env(name: str, default: str | None = None) -> str | None:
    value = os.getenv(name, default)
    if value is None:
        return None
    value = value.strip()
    return value if value else None


def _normalize_json_text(raw_text: str) -> str:
    obj = json.loads(raw_text)
    return json.dumps(obj, ensure_ascii=False)


def _selected_provider(payload: dict[str, Any]) -> str:
    provider = str(payload.get("provider") or _env("PROXY_PROVIDER", "openai")).strip().lower()
    if provider not in {"openai", "anthropic"}:
        raise HTTPException(status_code=400, detail="provider must be 'openai' or 'anthropic'")
    return provider


def _selected_system_prompt(payload: dict[str, Any]) -> str:
    return str(payload.get("system") or _env("PROXY_SYSTEM_PROMPT", DEFAULT_SYSTEM_PROMPT))


def _as_float(value: Any, default: float) -> float:
    if value is None:
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _as_int(value: Any, default: int) -> int:
    if value is None:
        return default
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


async def _call_openai(payload: dict[str, Any]) -> str:
    api_key = _env("OPENAI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY is not configured")

    base_url = _env("OPENAI_BASE_URL", "https://api.openai.com")
    model = str(payload.get("model") or _env("OPENAI_MODEL", "gpt-4o-mini"))
    timeout_seconds = _as_float(payload.get("timeout_seconds"), _as_float(_env("PROXY_TIMEOUT_SECONDS", "60"), 60.0))

    request_body = {
        "model": model,
        "messages": [
            {"role": "system", "content": _selected_system_prompt(payload)},
            {"role": "user", "content": payload["prompt"]},
        ],
        "temperature": _as_float(payload.get("temperature"), 0.0),
        "response_format": {"type": "json_object"},
    }

    if payload.get("max_tokens") is not None:
        request_body["max_tokens"] = _as_int(payload.get("max_tokens"), 512)

    try:
        async with httpx.AsyncClient(timeout=timeout_seconds) as client:
            response = await client.post(
                f"{base_url.rstrip('/')}/v1/chat/completions",
                headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
                json=request_body,
            )
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"OpenAI request failed: {exc}") from exc

    if response.status_code >= 400:
        raise HTTPException(status_code=response.status_code, detail=f"OpenAI error: {response.text}")

    data = response.json()
    content = data["choices"][0]["message"]["content"]
    if isinstance(content, list):
        content = "".join(part.get("text", "") for part in content if isinstance(part, dict))
    return _normalize_json_text(str(content))


async def _call_anthropic(payload: dict[str, Any]) -> str:
    api_key = _env("ANTHROPIC_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="ANTHROPIC_API_KEY is not configured")

    base_url = _env("ANTHROPIC_BASE_URL", "https://api.anthropic.com")
    model = str(payload.get("model") or _env("ANTHROPIC_MODEL", "claude-3-5-haiku-latest"))
    timeout_seconds = _as_float(payload.get("timeout_seconds"), _as_float(_env("PROXY_TIMEOUT_SECONDS", "60"), 60.0))

    request_body = {
        "model": model,
        "system": _selected_system_prompt(payload),
        "messages": [{"role": "user", "content": payload["prompt"]}],
        "temperature": _as_float(payload.get("temperature"), 0.0),
        "max_tokens": _as_int(payload.get("max_tokens"), 512),
    }

    try:
        async with httpx.AsyncClient(timeout=timeout_seconds) as client:
            response = await client.post(
                f"{base_url.rstrip('/')}/v1/messages",
                headers={
                    "x-api-key": api_key,
                    "anthropic-version": "2023-06-01",
                    "content-type": "application/json",
                },
                json=request_body,
            )
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Anthropic request failed: {exc}") from exc

    if response.status_code >= 400:
        raise HTTPException(status_code=response.status_code, detail=f"Anthropic error: {response.text}")

    data = response.json()
    text_parts = [part.get("text", "") for part in data.get("content", []) if part.get("type") == "text"]
    raw_text = "".join(text_parts)
    return _normalize_json_text(raw_text)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/interpret")
async def interpret(request: InterpretRequest) -> dict[str, Any]:
    payload = request.model_dump(exclude_none=True)
    if not payload.get("prompt"):
        raise HTTPException(status_code=400, detail="prompt is required")

    provider = _selected_provider(payload)
    if provider == "openai":
        normalized = await _call_openai(payload)
    else:
        normalized = await _call_anthropic(payload)

    return {"content": normalized}

