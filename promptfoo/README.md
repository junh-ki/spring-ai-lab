# Promptfoo POC suite

A YAML-driven evaluation suite for the same `/ai/generate` endpoint that
the [`deepeval/`](../deepeval) suite covers — but expressed as
[promptfoo](https://www.promptfoo.dev/) test cases. Useful when you want
declarative, language-agnostic tests that PMs can read and modify, plus a
nice HTML diff report between runs.

## What it covers

All cases target `GET /ai/generate?message=...&chatId=...`. Files are
organised by **what they verify**, not by HTTP shape. See
[`tests/README.md`](tests/README.md) for the full table.

| Category | Test file | What we score |
|---|---|---|
| Functional | [`tests/functional.yaml`](tests/functional.yaml) | happy-path correctness (factual, arithmetic, tone) with `llm-rubric` + deterministic `contains` + latency cap |
| Refusal | [`tests/refusal.yaml`](tests/refusal.yaml) | the model declines to fabricate information it does not have (PII, medical) |
| Safety | [`tests/safety.yaml`](tests/safety.yaml) | sensitive scenarios — crisis handling, self-harm signals. Stub; populate before presentation |
| Memory regression | [`tests/memory-regression.yaml`](tests/memory-regression.yaml) | does the second-turn reply recall facts from a priming turn sharing the same `chatId`? Requires `-j 1` |

Adversarial / red-team scanning is **deferred to a separate POC (Garak)** for
this project — see [`poc.md` §7.2](poc.md) for the rationale (data sovereignty
under regulated psychotherapy use case + accountability gating). Promptfoo's
`redteam` feature exists and is technically capable, but its email gate and
remote-default attack generation are incompatible with the offline-first
posture this POC is built on.

`/poem`, `/support`, `/agent/chat` are intentionally out of scope here —
the [`deepeval/`](../deepeval) suite already covers RAG and the pattern
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
├── regression-demo.sh           ← Demo 1 orchestrator (baseline / broken / restored)
├── regression-demo.md           ← live procedure for Demo 1
├── poc.md                       ← POC presentation template
├── tests/
│   ├── README.md                ← what each test file covers
│   ├── functional.yaml          ← happy-path verification (factual, arithmetic, tone)
│   ├── refusal.yaml             ← model declines to fabricate (PII, medical)
│   ├── safety.yaml              ← sensitive scenarios (crisis handling) — stub
│   └── memory-regression.yaml   ← memory through multi-turn — requires -j 1
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

### Pre-warming the models (recommended for live demos)

Ollama unloads idle models after ~5 minutes to free RAM. If you have not
run anything recently, the first eval call pays a cold-start tax of
~30-60s (SUT 1B model takes 5-15s to load, judge 8B takes 20-40s). This
is harmless for normal CI, annoying when demoing live, and adds noise to
duration measurements.

**Check current model state:**

```bash
~/.rd/bin/docker stats spring-ai-lab-ollama-1 --no-stream \
  --format 'RAM: {{.MemUsage}}'
```

| RAM reading | Meaning |
|---|---|
| < 2 GB | Models unloaded — first eval call will be slow |
| 5-7 GB | Models warm — runs start fast |

**Pre-warm explicitly before a manual run:**

```bash
# Pre-load SUT (Spring → llama3.2:1b)
curl -s -u user:demo \
  "http://localhost:8080/ai/generate?message=hi&chatId=_warmup" > /dev/null

# Pre-load judge (llama3.1:8b directly via Ollama)
curl -s -X POST http://127.0.0.1:11434/api/generate \
  -H 'Content-Type: application/json' \
  -d '{"model":"llama3.1:8b","prompt":"hi","stream":false}' > /dev/null
```

The first request triggers Ollama to load the model from disk into RAM
(takes 30-60s total for both); subsequent calls are fast until the
~5-minute idle window expires again.

> **Note:** `regression-demo.sh` does this automatically before each phase
> via the `warmup_models()` helper. Manual `./run.sh` invocations do NOT —
> pre-warm explicitly when speed matters (live demos, time-bound CI runs).

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

1. Pick the right file by category — see [`tests/README.md`](tests/README.md):
   - `functional.yaml` for stateless happy-path cases
   - `refusal.yaml` if the model must decline (PII, medical, etc.)
   - `safety.yaml` for crisis / self-harm / abuse scenarios
   - `memory-regression.yaml` if the case needs multi-turn memory
2. Append a row with `description`, `vars.prompt`, a unique `vars.chatId`,
   and an `assert:` block. Cheap asserts first (`contains`, `latency`), then
   `llm-rubric` for semantic checks.
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
