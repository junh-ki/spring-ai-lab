# DeepEval POC suite

A small, **fully offline** evaluation harness for the demo endpoints exposed by `spring-ai-lab`. It uses [DeepEval](https://docs.confident-ai.com/) — an LLM-as-judge framework — wired to the same local Ollama instance the app under test already runs against.

## What it covers

Only endpoints whose body **is** the model's answer are worth evaluating. The README at the repo root lists four; this POC focuses on the two with the most interesting eval shape:

| Endpoint | Test file | What we score |
|---|---|---|
| `GET /ai/generate?message=...&chatId=...` (single-turn) | [tests/test_chat.py](tests/test_chat.py) | answer relevancy + correctness vs. a gold reference |
| `GET /ai/generate?message=...&chatId=...` (multi-turn) | [tests/test_chat_memory.py](tests/test_chat_memory.py) | does the final reply recall facts from earlier turns sharing the same `chatId`? |
| `GET /support?question=...` (RAG over the demo PDF) | [tests/test_support_rag.py](tests/test_support_rag.py) | faithfulness + relevancy + correctness of grounded answers |

`/poem` and `/agent/chat` are intentionally out of scope for this POC — they are easy to add following the same pattern.

## How it works

```
┌────────────────────────┐    HTTP (Basic auth)    ┌────────────────────────────┐
│ pytest test (golden)   │ ──────────────────────► │ Spring app (system under   │
│                        │                         │ test) — Ollama-backed      │
│                        │ ◄────── actual_output ──│                            │
│                        │                         └────────────────────────────┘
│                        │
│                        │    DeepEval metric      ┌────────────────────────────┐
│                        │ ──────────────────────► │ OllamaJudge (LLM-as-judge) │
│                        │                         │ same Ollama, judge model   │
│                        │ ◄──── score + reason ───│ chosen via env var         │
└────────────────────────┘                         └────────────────────────────┘
```

- The **system under test** is the Spring app, queried over HTTP. The judge never sees the system's internals — only request/response pairs, which is exactly how a black-box production eval would work.
- The **judge** is a separate Ollama call. By default it reuses `OLLAMA_MODEL` (the demo's `llama3.2:1b`); you can point it at a stronger local model with `DEEPEVAL_JUDGE_MODEL`.
- DeepEval's built-in metrics (`AnswerRelevancyMetric`, `FaithfulnessMetric`, `GEval`) handle the actual scoring — we only provide the LLM wrapper, the goldens, and the test wiring.

### Layout

```
deepeval/
├── README.md                    ← this file
├── requirements.txt             ← Python deps (deepeval, pytest, requests, pydantic)
├── pytest.ini                   ← pythonpath so `from datasets...` works
├── run.sh                       ← venv + install + pytest
├── conftest.py                  ← session fixtures: health check, PDF ingest
├── ollama_judge.py              ← DeepEvalBaseLLM impl talking to Ollama
├── spring_client.py             ← thin requests wrapper for the demo endpoints
├── datasets/
│   ├── chat_goldens.py          ← single-turn /ai/generate cases
│   ├── memory_goldens.py        ← multi-turn /ai/generate cases (chatId reuse)
│   └── rag_goldens.py           ← /support cases grounded in the demo PDF
└── tests/
    ├── test_chat.py
    ├── test_chat_memory.py
    └── test_support_rag.py
```

## Running it

**Prerequisites:**

- **Python 3.10+** — DeepEval uses PEP 604 union syntax (`X | None`), which 3.9 (the default on macOS) does not support. Install a newer one:
  ```bash
  brew install python@3.12          # macOS
  # or with pyenv:
  pyenv install 3.12.6 && pyenv shell 3.12.6
  ```
  `run.sh` auto-detects `python3.10` … `python3.13` on PATH and bails with a clear message if none are found.
- The demo running locally (Ollama on `:11434`, Spring on `:8080`):
  ```bash
  ./start-demo.sh         # from repo root, leaves running in foreground
  ```

In a second terminal:

```bash
cd deepeval
./run.sh                  # creates .venv, installs deps, runs pytest
```

`run.sh` is just a convenience wrapper. To do the same steps manually:

```bash
cd deepeval
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export DEEPEVAL_TELEMETRY_OPT_OUT=YES
pytest
```

Pass any pytest flag through `run.sh`:

```bash
./run.sh tests/test_chat_memory.py        # one file
./run.sh -k remember_user_name            # one case
./run.sh -x                               # stop on first failure
```

## Configuration

All knobs are env vars (sane defaults for `./start-demo.sh`):

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_BASE_URL` | `http://localhost:8080` | demo app base URL |
| `SPRING_USER` / `SPRING_PASSWORD` | `user` / `demo` | Basic auth (matches `application-dev.yaml`) |
| `OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Ollama for the judge |
| `OLLAMA_MODEL` | `llama3.2:1b` | falls through as judge model if `DEEPEVAL_JUDGE_MODEL` is unset |
| `DEEPEVAL_JUDGE_MODEL` | _unset_ | **strongly recommended:** override to a more capable local model (see caveat below) |
| `DEEPEVAL_JUDGE_TIMEOUT` | `180` | per-judge-call timeout, seconds |
| `SKIP_RAG_INGEST` | _unset_ | set to `1` to skip the session-scoped PDF ingest (use when you've already ingested out of band) |

### About the local judge — read this

`llama3.2:1b` is **fast but a noisy judge**. DeepEval metrics rely on the judge to follow long instructions, return strict JSON, and reason about subtle differences. A 1B model will sometimes:

- mark obviously-correct answers as failures,
- mis-parse the JSON schema and crash a metric,
- give wildly different scores on the same case across runs.

For a credible POC, pull a stronger local model and point the judge at it:

```bash
docker exec -it $(docker ps --filter ancestor=ollama/ollama --format '{{.ID}}') \
  ollama pull llama3.1:8b
DEEPEVAL_JUDGE_MODEL=llama3.1:8b ./run.sh
```

(Or `qwen2.5:7b`, `mistral:7b`, etc.) The system under test stays on `llama3.2:1b` — only the judge gets upgraded. This is the [option (a)](#) "fully local" tradeoff: free and offline, but the judge quality is the ceiling on score reliability.

## How the goldens are designed

### Chat (`datasets/chat_goldens.py`)
Short factual questions with terse `expected_output`. Scored with `AnswerRelevancyMetric` + a `GEval` *Correctness* metric that compares actual to expected. Each test gets a unique `chatId` so single-turn cases don't bleed memory between tests.

### Chat memory (`datasets/memory_goldens.py`)
A `MemoryGolden` is a list of turns sharing one `chatId`. The first N-1 turns *prime* the conversation (no scoring); only the last turn is scored, and the metric explicitly asks the judge whether the response surfaced the fact that was established earlier. If memory is broken (e.g. Redis misconfigured, advisor not registered), every memory test fails — this is exactly the regression we want to catch.

### RAG (`datasets/rag_goldens.py`)
Questions grounded in [`AI Platform Team - Spring AI Intro.pdf`](../AI%20Platform%20Team%20-%20Spring%20AI%20Intro.pdf), which `conftest.py` ingests once per session via `POST /etl/import-pdf`. Each golden carries an `expected_output` and a `retrieval_context` — the latter is what `FaithfulnessMetric` compares the answer against.

> **Known POC simplification:** `/support` doesn't return what was actually retrieved from the vector store, so we substitute the *expected* excerpts. This means Faithfulness is technically scoring "is the answer faithful to the ground truth?" rather than "is the answer faithful to what RAG actually retrieved?". For a production eval, expose the retrieved chunks via a debug endpoint and feed *those* into `retrieval_context`.

## Adding a new test

1. Add a golden to the matching dataset (or create a new one).
2. If you need a new endpoint, add a method to `spring_client.py`.
3. Either add a parametrized case to an existing test file or create a new `tests/test_*.py` that follows the same shape:
   - get `actual_output` from the Spring client,
   - build an `LLMTestCase`,
   - pick metric(s) and pass `model=judge` + `async_mode=False`,
   - call `assert_test`.

`async_mode=False` matters: our `OllamaJudge` proxies async calls through the sync path, and turning DeepEval's parallelism off keeps the failure modes legible.

## Troubleshooting

- **`Spring app not reachable at http://localhost:8080`** — start `./start-demo.sh` first; the fixture `pytest.exit`s rather than running with a dead backend.
- **Every metric fails with JSON parse errors** — the judge model is too small. Switch `DEEPEVAL_JUDGE_MODEL` to a 7B+ model.
- **`/support` answers are vague or empty** — the PDF didn't ingest. Check Spring logs for `Processed N items safely.` from `IdempotentIngestionService`. Re-run with `SKIP_RAG_INGEST=` unset.
- **Memory tests pass but you don't trust them** — bump `app.chat.memory-retrieve-size` in `application.yaml`, or inspect Redis directly: `docker exec -it <redis-container> redis-cli KEYS '*'`.
- **DeepEval prompts to log into Confident AI** — `export DEEPEVAL_TELEMETRY_OPT_OUT=YES` (`run.sh` sets this for you).
