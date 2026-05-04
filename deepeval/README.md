# DeepEval POC suite

A small, **fully offline** evaluation harness for the `GET /ai/generate` endpoint exposed by `spring-ai-lab`. It uses [DeepEval](https://docs.confident-ai.com/) — an LLM-as-judge framework — wired to the same local Ollama instance the app under test already runs against.

## What it covers

The suite targets a single endpoint — `GET /ai/generate?message=...&chatId=...` — and exercises two aspects of it:

| Aspect | Test file | How we check |
|---|---|---|
| Single-turn quality | [tests/test_chat.py](tests/test_chat.py) | DeepEval `AnswerRelevancyMetric` (LLM-judged) **+** deterministic substring assertion on the golden keyword (e.g. `"Paris"`) |
| Multi-turn coherence (chat memory) | [tests/test_chat_memory.py](tests/test_chat_memory.py) | deterministic substring assertion: does the final reply contain the recall keyword (e.g. `"Junhyung"`) seeded in earlier turns sharing the same `chatId`? |

The memory test is the chat-history coherence check: it primes a conversation under one `chatId`, then asserts the next reply on the same `chatId` surfaces the seeded fact.

**Why mixed (LLM + deterministic)?** A 1B local judge is too noisy to score exact-match answers reliably — it has been observed to fail clearly-correct answers because *phrasing* differs from the reference. For a POC we use the LLM judge only where it adds value (relevancy), and use simple substring assertions for correctness/recall, where the answer is unambiguous. Drop in `GEval` correctness later when you have a stronger judge.

## How it works

```
┌────────────────────────┐    HTTP (Basic auth)    ┌────────────────────────────┐
│ pytest test (golden)   │ ──────────────────────► │ Spring app (system under   │
│                        │     /ai/generate        │ test) — Ollama-backed      │
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
- DeepEval's `AnswerRelevancyMetric` handles the LLM-judged side; correctness/recall use plain substring assertions to stay POC-friendly.

### Layout

```
deepeval/
├── README.md                    ← this file
├── requirements.txt             ← Python deps (deepeval, pytest, requests, pydantic)
├── pytest.ini                   ← pythonpath so `from datasets...` works
├── run.sh                       ← venv + install + pytest
├── conftest.py                  ← session fixtures: health check, judge
├── ollama_judge.py              ← DeepEvalBaseLLM impl talking to Ollama
├── spring_client.py             ← thin requests wrapper for /ai/generate
├── datasets/
│   ├── chat_goldens.py          ← single-turn /ai/generate cases
│   └── memory_goldens.py        ← multi-turn /ai/generate cases (chatId reuse)
└── tests/
    ├── test_chat.py
    └── test_chat_memory.py
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

(Or `qwen2.5:7b`, `mistral:7b`, etc.) The system under test stays on `llama3.2:1b` — only the judge gets upgraded.

## How the goldens are designed

### Chat (`datasets/chat_goldens.py`)
Short factual questions. Each golden carries an `expected_substring` — the unambiguous keyword that *must* appear in the response (e.g. `"Paris"`). The test does a case-insensitive substring assertion **and** runs `AnswerRelevancyMetric` to confirm the response is on-topic. Each test gets a unique `chatId` so single-turn cases don't bleed memory between tests.

### Chat memory (`datasets/memory_goldens.py`)
A `MemoryGolden` is a list of turns sharing one `chatId`. The first N-1 turns *prime* the conversation; only the last turn is checked. The check is a case-insensitive substring assertion that the response contains `expected_recall` (e.g. `"Junhyung"`, `"teal"`, `"Lisbon"`). If memory is broken (advisor not registered, conversation ID not threaded), every memory test fails — this is exactly the regression we want to catch.

> The demo's chat memory is in-memory (`InMemoryChatMemoryRepository`) and resets on app restart. Run the suite against the same running process; don't restart the app between tests.

## Adding a new test

1. Add a golden to the matching dataset (or create a new one).
2. Either add a parametrized case to an existing test file or create a new `tests/test_*.py` that follows the same shape:
   - get `actual_output` from `spring_client.generate(...)`,
   - assert the expected substring is present (deterministic correctness),
   - optionally build an `LLMTestCase` and run `AnswerRelevancyMetric` (LLM-judged on-topic check) — pass `model=judge` + `async_mode=False`,
   - call `assert_test`.

`async_mode=False` matters: our `OllamaJudge` proxies async calls through the sync path, and turning DeepEval's parallelism off keeps the failure modes legible.

## Troubleshooting

- **`Spring app not reachable at http://localhost:8080`** — start `./start-demo.sh` first; the fixture `pytest.exit`s rather than running with a dead backend.
- **Every metric fails with JSON parse errors** — the judge model is too small. Switch `DEEPEVAL_JUDGE_MODEL` to a 7B+ model.
- **Memory tests fail unexpectedly** — confirm the app wasn't restarted mid-suite (the in-memory store is wiped on restart). If it persists, bump `app.chat.memory-retrieve-size` in `application.yaml`.
- **DeepEval prompts to log into Confident AI** — `export DEEPEVAL_TELEMETRY_OPT_OUT=YES` (`run.sh` sets this for you).
