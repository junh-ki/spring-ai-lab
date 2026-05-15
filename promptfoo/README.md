# Promptfoo POC suite

A YAML-driven evaluation suite for the same `/ai/generate` endpoint that
the [`deepeval/`](../deepeval) suite covers — but expressed as
[promptfoo](https://www.promptfoo.dev/) test cases. Useful when you want
declarative, language-agnostic tests that PMs can read and modify, plus a
nice HTML diff report between runs.

## What it covers

All cases target `GET /ai/generate?message=...&chatId=...`. Files are
organised by **what they verify**, not by HTTP shape.

| Category | Test file | What it verifies | Stateless? | Run hint |
|---|---|---|---|---|
| Functional | [`tests/functional.yaml`](tests/functional.yaml) | Happy-path correctness (factual, arithmetic, tone, length). | yes | any concurrency |
| Refusal | [`tests/refusal.yaml`](tests/refusal.yaml) | The model declines to fabricate information it does not have (PII, medical / legal). | yes | any concurrency |
| Safety | [`tests/safety.yaml`](tests/safety.yaml) | Sensitive scenarios at moderate intensity (everyday emotional distress, low mood, ambiguous symptoms) — explicitly excludes crisis prompts (out of scope for this POC). | yes | any concurrency |
| Memory regression | [`tests/memory-regression.yaml`](tests/memory-regression.yaml) | Memory through multi-turn — does the second-turn reply recall facts from a priming turn sharing the same `chatId`? | **no — stateful** | **`-j 1` required** |

Adversarial / red-team scanning is out of scope for this suite — delegated to Garak.

`/poem`, `/support`, `/agent/chat` are intentionally out of scope here —
the [`deepeval/`](../deepeval) suite already covers RAG and the pattern
is the same.

## Layout

```
promptfoo/
├── README.md                    ← this file (operational)
├── poc.md                       ← POC plan, hypotheses, findings, diagrams
├── promptfooconfig.yaml         ← provider, grader, test includes
├── run.sh                       ← wrapper: pre-flight + npx promptfoo eval
├── regression-demo.sh           ← Demo 1 orchestrator (baseline / broken / restored)
└── tests/
    ├── functional.yaml          ← happy-path verification
    ├── refusal.yaml             ← model declines to fabricate
    ├── safety.yaml              ← sensitive scenarios at moderate intensity
    └── memory-regression.yaml   ← memory through multi-turn — requires -j 1
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
./run.sh                            # all tests, writes report/last.html
./run.sh --filter-pattern memory    # only multi-turn tests
./run.sh --filter-pattern factual   # one row (description match)
./run.sh --no-cache                 # skip cached judge calls (required for stateful tests)
./run.sh -j 1                       # serial — required for memory tests
```

### Reading the results

Two complementary ways to inspect a run:

| Method | What you get | When to use |
|---|---|---|
| `open report/last.html` | Static HTML snapshot of the most recent run | Quick look at the most recent eval |
| `npx promptfoo@latest view` (or `promptfoo view` if installed globally) | Local web dashboard listing **all stored runs** with diff between any two, history, search | Comparing baseline vs broken vs restored from the regression demo, trend analysis, stakeholder review |

The `view` command starts a local web server (default `http://localhost:15500`)
and opens your browser. Stop it with `Ctrl+C`. Use this when presenting to
non-engineers — the dashboard is far more legible than a single static page.

### Pre-warming the models (recommended for live demos)

Ollama unloads idle models after ~5 minutes; the first eval after that
pays a 30-60s cold-start tax. Harmless for CI, annoying when demoing live.

```bash
# SUT
curl -s -u user:demo "http://localhost:8080/ai/generate?message=hi&chatId=_warmup" > /dev/null
# Judge
curl -s -X POST http://127.0.0.1:11434/api/generate \
  -H 'Content-Type: application/json' \
  -d '{"model":"llama3.1:8b","prompt":"hi","stream":false}' > /dev/null
```

`regression-demo.sh` pre-warms automatically. Manual `./run.sh` does not.

## Regression demo

The `regression-demo.sh` script runs the suite three times in sequence
to demonstrate that disabling the chat-memory advisor causes memory tests
to fail, then restoring it brings them back.

### Setup principle — feature flag, not code edit

The Spring AI app exposes a documented configuration toggle
(`app.demo.memory-disabled`, set via `APP_DEMO_MEMORY_DISABLED` env var).
When `true`, the primary `ChatClient` bean is built **without** the
`MessageChatMemoryAdvisor`. Default is `false`; a `WARN` log fires on
startup whenever the flag is active.

This matters because:

- **No code edits at demo time.** The codebase is never temporarily broken.
- **Reproducible.** Anyone can replay the demo with the same env var.
- **Auditable.** The startup `WARN` log records that the flag was active.
- **CI-ready.** Future CI jobs can enforce expected pass/fail under each flag.

Flag wiring lives in [`ChatClientConfig.java`](../src/main/java/com/example/springailab/config/ChatClientConfig.java).

### Run it

Prerequisites beyond the normal run: Spring AI running (`./start-demo.sh`),
judge model `llama3.1:8b` pulled in Ollama, optionally `jq` installed for
pass/fail counts in the summary.

```bash
cd promptfoo
./regression-demo.sh
```

The script runs three evaluations and pauses between them for you to
restart Spring AI with the appropriate flag:

```
Phase 1 — Baseline (memory enabled)        → expects 4/4 PASS
  [PAUSE] Stop start-demo.sh, relaunch with:
          APP_DEMO_MEMORY_DISABLED=true ./start-demo.sh
Phase 2 — Broken (memory disabled)         → expects multi-turn checks to FAIL
  [PAUSE] Stop start-demo.sh, relaunch normally:
          ./start-demo.sh
Phase 3 — Restored (memory enabled again)  → expects 4/4 PASS
```

Reports saved to `report/regression-<ts>/` as `01-baseline.{html,json}`,
`02-broken.{html,json}`, `03-restored.{html,json}`.

### What to highlight in the reports

For each multi-turn checking row (`memory[A] check`, `memory[B] check`):

| Report | Verdict | What the SUT response looks like |
|---|---|---|
| `01-baseline.html` | **PASS** | *"Your name is Alice."* — confident recall |
| `02-broken.html` | **FAIL** | *"I don't have any information about you, this conversation just started"* |
| `03-restored.html` | **PASS** | *"Your name is Alice."* again |

The SUT model is unchanged in all three phases — only the advisor differs.
A liveness check would not have caught this regression because the endpoint
returns 200 in all three phases. The judge's reasoning, surfaced inline in
the HTML report, is what differentiates this from a unit test that would
just say "expected X, got Y".

### Safety notes

- **Never set `APP_DEMO_MEMORY_DISABLED=true` outside demo/CI scenarios**,
  and don't commit `.env` or run-scripts that hardcode it. The startup
  `WARN` log is intentionally loud — treat it as a tripwire.
- **Restart hygiene.** Forgetting to restart Spring after restoring the
  flag will leave the audience confused — validate with the final green
  Phase 3 run before declaring the demo over. The script does this for you.

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

### About the local judge

`llama3.2:1b` is **fast but a noisy `llm-rubric` grader**. It will sometimes
mark obviously-correct answers as failures, mis-parse the required JSON, or
score the same case differently across runs. Same caveat as `deepeval/` —
"small model + strict rubric = unreliable".

For a credible POC, pull a stronger local model and edit the provider id:

```bash
docker exec -it $(docker ps --filter ancestor=ollama/ollama --format '{{.ID}}') \
  ollama pull llama3.1:8b
# then change `id: openai:chat:llama3.2:1b` -> `id: openai:chat:llama3.1:8b`
```

## Adding a new test or category

To add a **new case to an existing category**:

1. Pick the right file by category:
   - `functional.yaml` for stateless happy-path cases
   - `refusal.yaml` if the model must decline (PII, medical, etc.)
   - `safety.yaml` for moderate-intensity sensitive scenarios
   - `memory-regression.yaml` if the case needs multi-turn memory
2. Append a row with `description`, `vars.prompt`, a unique `vars.chatId`,
   and an `assert:` block. Cheap asserts first (`contains`, `latency`),
   then `llm-rubric` for semantic checks.
3. Re-run `./run.sh`. Inspect via `report/last.html` for a quick view of
   the latest run, or `promptfoo view` for the local dashboard with run
   history (useful when comparing the new case across multiple iterations).
   Both surface pass/fail per row with the judge's reasoning inline — see
   [Reading the results](#reading-the-results).

To add a **new category** (only when introducing genuinely new behavior):

1. Create `tests/<category>.yaml` with a header comment explaining what the
   file verifies (one paragraph).
2. Add the file to `promptfooconfig.yaml` under `tests:`.
3. Update the "What it covers" table at the top of this README.
4. If the category requires special run flags (like `memory-regression`
   needs `-j 1`), call that out explicitly in the table.

## Troubleshooting

- **`Spring app not reachable`** in `run.sh`'s pre-flight — start the demo
  first with `./start-demo.sh`.
- **All `llm-rubric` asserts fail with parse errors** — judge model is too
  small. Switch to a 7B+ model.
- **Multi-turn tests don't see memory** — the rows must share `chatId` AND
  run in order. Promptfoo runs tests sequentially per provider by default;
  if you set `-j N` higher, the priming turn might not finish first. Stick
  with `-j 1` for memory tests.
- **`npx` re-downloads promptfoo every run** — install it once globally
  (`npm i -g promptfoo`) and replace `npx --yes promptfoo@latest` with
  `promptfoo` in `run.sh`.