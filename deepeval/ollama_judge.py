"""
Custom DeepEval LLM that evaluates against a local Ollama instance.

DeepEval uses an LLM-as-judge to score every metric. By default it calls OpenAI;
this wrapper redirects all judge calls to the same Ollama process the demo app
uses (so the POC runs fully offline).

The judge model is independent of the model under test. Override via the
DEEPEVAL_JUDGE_MODEL env var. `llama3.2:1b` (the demo default) is fast but a
weak judge — pull `llama3.1:8b` or `qwen2.5:7b` for more reliable scores.
"""
from __future__ import annotations

import json
import os
from typing import Any, Optional, Type

import requests
from deepeval.models.base_model import DeepEvalBaseLLM
from pydantic import BaseModel


OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
JUDGE_MODEL = os.environ.get("DEEPEVAL_JUDGE_MODEL", os.environ.get("OLLAMA_MODEL", "llama3.2:1b"))
REQUEST_TIMEOUT_S = int(os.environ.get("DEEPEVAL_JUDGE_TIMEOUT", "180"))


class OllamaJudge(DeepEvalBaseLLM):
    """LLM-as-judge backed by Ollama's /api/chat endpoint."""

    def __init__(self, model: str = JUDGE_MODEL, base_url: str = OLLAMA_BASE_URL) -> None:
        self.model = model
        self.base_url = base_url

    def load_model(self) -> str:
        return self.model

    def get_model_name(self) -> str:
        return f"ollama:{self.model}"

    def generate(self, prompt: str, schema: Optional[Type[BaseModel]] = None) -> Any:
        payload: dict[str, Any] = {
            "model": self.model,
            "messages": [{"role": "user", "content": self._wrap(prompt, schema)}],
            "stream": False,
            "options": {"temperature": 0.0},
        }
        if schema is not None:
            # Ollama supports a "format" field that constrains output to JSON when set to "json",
            # or to a JSON Schema dict for stricter structured output. Pydantic v2 ships
            # model_json_schema() — use it directly.
            payload["format"] = schema.model_json_schema()

        response = requests.post(
            f"{self.base_url}/api/chat",
            json=payload,
            timeout=REQUEST_TIMEOUT_S,
        )
        response.raise_for_status()
        content = response.json()["message"]["content"]
        return self._coerce(content, schema)

    async def a_generate(self, prompt: str, schema: Optional[Type[BaseModel]] = None) -> Any:
        # DeepEval calls a_generate inside its own asyncio loop. We don't have a real async
        # Ollama client here, so we delegate to the sync path. The judge calls are I/O bound
        # and DeepEval batches them, so this stays acceptable for POC scale.
        return self.generate(prompt, schema)

    @staticmethod
    def _wrap(prompt: str, schema: Optional[Type[BaseModel]]) -> str:
        if schema is None:
            return prompt
        return (
            f"{prompt}\n\n"
            f"You MUST respond with a single JSON object matching this schema "
            f"(no prose, no code fences):\n{json.dumps(schema.model_json_schema())}"
        )

    @staticmethod
    def _coerce(content: str, schema: Optional[Type[BaseModel]]) -> Any:
        if schema is None:
            return content
        text = content.strip()
        # Some models still wrap JSON in fences despite format=json — strip defensively.
        if text.startswith("```"):
            text = text.strip("`")
            if "\n" in text:
                text = text.split("\n", 1)[1]
            text = text.rsplit("```", 1)[0].strip()
        return schema.model_validate_json(text)
