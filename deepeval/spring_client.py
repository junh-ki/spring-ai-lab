"""HTTP client for the spring-ai-lab `/ai/generate` endpoint under evaluation."""
from __future__ import annotations

import os
from typing import Any

import requests


SPRING_BASE_URL = os.environ.get("SPRING_BASE_URL", "http://localhost:8080").rstrip("/")
SPRING_USER = os.environ.get("SPRING_USER", "user")
SPRING_PASSWORD = os.environ.get("SPRING_PASSWORD", "demo")
HTTP_TIMEOUT_S = int(os.environ.get("SPRING_HTTP_TIMEOUT", "180"))


class SpringClient:
    def __init__(
        self,
        base_url: str = SPRING_BASE_URL,
        user: str = SPRING_USER,
        password: str = SPRING_PASSWORD,
    ) -> None:
        self.base_url = base_url
        self.session = requests.Session()
        self.session.auth = (user, password)
        # Spring app is always on localhost; bypass any HTTP(S)_PROXY env vars
        # that would otherwise route the request through a corporate proxy.
        self.session.trust_env = False

    def health(self) -> bool:
        try:
            response = self.session.get(
                f"{self.base_url}/ai/generate",
                params={"message": "ping", "chatId": "_health"},
                timeout=15,
            )
            return response.status_code == 200
        except requests.RequestException:
            return False

    def generate(self, message: str, chat_id: str = "default") -> str:
        response = self.session.get(
            f"{self.base_url}/ai/generate",
            params={"message": message, "chatId": chat_id},
            timeout=HTTP_TIMEOUT_S,
        )
        response.raise_for_status()
        body: dict[str, Any] = response.json()
        return body["generation"]
