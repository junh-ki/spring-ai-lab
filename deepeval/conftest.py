"""Shared pytest fixtures for the DeepEval POC suite.

Responsibilities:
  - Verify the Spring Boot demo is reachable before any test runs.
  - Ingest the sample PDF once per session so /support has something to retrieve.
  - Provide a shared SpringClient and OllamaJudge to every test.
"""
from __future__ import annotations

import os
from pathlib import Path

import pytest

from ollama_judge import OllamaJudge
from spring_client import SpringClient


REPO_ROOT = Path(__file__).resolve().parent.parent
SAMPLE_PDF = REPO_ROOT / "AI Platform Team - Spring AI Intro.pdf"


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


@pytest.fixture(scope="session")
def rag_corpus_ready(spring_client: SpringClient) -> Path:
    """Ingest the sample PDF once per session.

    Idempotent on the server side (IdempotentMetadataEnricher hashes chunks),
    so re-running the suite does not create duplicates. Set
    SKIP_RAG_INGEST=1 to skip when you've already ingested out-of-band.
    """
    if os.environ.get("SKIP_RAG_INGEST") == "1":
        return SAMPLE_PDF
    if not SAMPLE_PDF.exists():
        pytest.exit(f"Sample PDF not found at {SAMPLE_PDF}", returncode=2)
    spring_client.import_pdf(SAMPLE_PDF)
    return SAMPLE_PDF
