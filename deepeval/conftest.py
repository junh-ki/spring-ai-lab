"""Shared pytest fixtures for the DeepEval POC suite.

Responsibilities:
  - Verify the Spring Boot demo is reachable before any test runs.
  - Provide a shared SpringClient and OllamaJudge to every test.
"""
from __future__ import annotations

import pytest

from ollama_judge import OllamaJudge
from spring_client import SpringClient


@pytest.fixture(scope="session")
def spring_client() -> SpringClient:
    client = SpringClient()
    if not client.health():
        pytest.exit(
            f"Spring app not reachable at {client.base_url}. "
            f"Start it first with ./start-demo.sh from the repo root.",
            returncode=2,
        )
    return client


@pytest.fixture(scope="session")
def judge() -> OllamaJudge:
    return OllamaJudge()
