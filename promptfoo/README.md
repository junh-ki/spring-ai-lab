# Promptfoo POC suite

A YAML-driven evaluation suite for the same `/ai/generate` endpoint that
the [`deepeval/`](../deepeval/) suite covers — but expressed as
[promptfoo](https://www.promptfoo.dev/) test cases. Useful when you want
declarative, language-agnostic tests that PMs can read and modify, plus a
nice HTML diff report between runs.

## What it covers

| Endpoint | Test file | What we score |
|---|---|---|
| `GET /ai/generate?message=...&chatId=...` (single-turn) | [tests/single_turn.yaml](tests/single_turn.yaml) | answer relevancy + correctness via `llm-rubric`, deterministic `contains`, latency cap |
| `GET /ai/generate?message=...&chatId=...` (multi-turn) | [tests/multi_turn.yaml](tests/multi_turn.yaml) | does the second-turn reply recall facts from a priming turn that shares the same `chatId`? |

`/poem`, `/support`, `/agent/chat` are intentionally out of scope here —
the [`deepeval/`](../deepeval/) suite already covers RAG and the pattern
is the same.

## How it works

```
┌────────────────────────┐  HTTP (Basic auth)   ┌────────────────────────────┐
│ promptfoo eval         │ ──────────────────► │ Spring app (system under   │
│ (declarative tests)    │                      │ test) — Ollama-backed      │
│                        │ ◄───── generation ── │                            │
│                        │                      └────────────────────────────┘
│                        │
│                        │  llm-rubric grading  ┌────────────────────────────┐
│                        │ ──────────────────► │ Ollama via OpenAI-compat   │
│                        │                      │ /v1 (judge model)          │
│                        │ ◄──── score ──────── │                            │
└────────────────────────┘                      └────────────────────────────┘
```

- **SUT**: same Spring app as the demo, hit over HTTP. promptfoo's `http`
  provider templates `{{prompt}}` and `{{vars.chatId}}` directly into the URL,
  so multi-turn is just "two rows that share a chatId".
- **Judge**: Ollama exposes an OpenAI-compatible API at `/v1`. The
  `defaultTest.options.provider` block points the grader at it. By default
  this reuses the demo's `llama3.2:1b`; a 7B+ judge is strongly recommended.
- **Assertions**: deterministic ones (`contains`, `latency`) run for free;
  `llm-rubric` adds an LLM call per assertion.

### Layout

```
promptfoo/
├── README.md                    ← this file
├── promptfooconfig.yaml         ← provider, grader, test includes
├── run.sh                       ← wrapper: pre-flight + npx promptfoo eval
├── tests/
│   ├── single_turn.yaml         ← one-shot Q&A cases
│   └── multi_turn.yaml          ← memory recall via shared chatId
└── .gitignore
```

## Running it

**Prerequisites:**
- Node.js 20+ (`brew install node`). The wrapper uses `npx --yes promptfoo@latest`.
- The demo running locally:
  ```bash
  ./start-demo.sh         # from repo root
  ```

In a second terminal:

```bash
cd promptfoo
./run.sh                  # runs all tests, writes ./report/last.html
```

Pass any promptfoo flag through:

```bash
./run.sh --filter-pattern memory     # only multi-turn tests
./run.sh --no-cache                  # skip cached judge calls
./run.sh -j 1                        # serial (debug judge non-determinism)
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_BASE_URL` | `http://localhost:8080` | demo app base URL |
| `SPRING_USER` / `SPRING_PASSWORD` | `user` / `demo` | Basic auth (matches `application-dev.yaml`) |
| `OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Ollama for the judge |
| `OLLAMA_MODEL` | `llama3.2:1b` | judge model |

If you want a different judge model, edit `promptfooconfig.yaml` →
`defaultTest.options.provider.id` (e.g. `openai:chat:llama3.1:8b`) and
make sure that model is pulled in Ollama.

### About the local judge — read this

`llama3.2:1b` is **fast but a noisy `llm-rubric` grader**. It will
sometimes mark obviously-correct answers as failures, mis-parse the
required JSON, or score the same case differently across runs. This is
the same caveat that applies to `deepeval/` — the underlying problem is
just "small model + strict rubric = unreliable".

For a credible POC, pull a stronger local model and edit the provider id:

```bash
docker exec -it $(docker ps --filter ancestor=ollama/ollama --format '{{.ID}}') \
  ollama pull llama3.1:8b
# then change `id: openai:chat:llama3.2:1b` -> `id: openai:chat:llama3.1:8b`
```

## Adding a new test

1. Pick a test file (`tests/single_turn.yaml` for stateless cases,
   `tests/multi_turn.yaml` if it needs memory).
2. Append a row with `description`, `vars.prompt`, a unique
   `vars.chatId`, and an `assert:` block. Cheap asserts first
   (`contains`, `latency`), then `llm-rubric` for semantic checks.
3. Re-run `./run.sh`. The HTML report at `report/last.html` shows pass/fail
   per row with the judge's reasoning inline.

## Troubleshooting

- **`Spring app not reachable`** in `run.sh`'s pre-flight — start the demo
  first with `./start-demo.sh`.
- **All `llm-rubric` asserts fail with parse errors** — judge model is too
  small. Switch to a 7B+ model.
- **Multi-turn tests don't see memory** — the rows must share `chatId` AND
  run in order. promptfoo runs tests sequentially per provider by default;
  if you set `-j N` higher, the priming turn might not finish first. Stick
  with `-j 1` for memory tests.
- **`npx` re-downloads promptfoo every run** — install it once globally
  (`npm i -g promptfoo`) and replace `npx --yes promptfoo@latest` with
  `promptfoo` in `run.sh`.
