# Demo 1 — Regression replay (memory advisor disabled)

> Live procedure for the POC presentation. Demonstrates that promptfoo catches
> a high-impact regression — disabling the chat-memory advisor — in seconds,
> with one orchestration script that produces three timestamped reports
> (baseline, broken, restored).

## What this demo proves

A single line of YAML in [`tests/multi_turn.yaml`](tests/multi_turn.yaml) encodes
a real production requirement (*"the assistant must remember what the user said
earlier in the same conversation"*). When the memory advisor is disabled —
intentionally or by accident during a refactor — promptfoo catches it on the
next run and the judge spells out *why* the test failed in human-readable form.

This is the **single most valuable use case for promptfoo** in a CI/CD pipeline:
protecting behavior that would otherwise require manual end-to-end testing of
every endpoint after every commit.

## Setup principle — feature flag, not code edit

The Spring AI app exposes a documented configuration toggle:

```
app.demo.memory-disabled
```

When set to `true` (via the `APP_DEMO_MEMORY_DISABLED` environment variable),
the primary `ChatClient` bean is built **without** the
`MessageChatMemoryAdvisor`. The default is `false` and a `WARN` log is emitted
on startup whenever the flag is active, so the demo mode is impossible to miss
in any environment.

Why this matters:

- **No code edits at demo time.** The codebase is never temporarily broken.
- **Reproducible.** Anyone can replay the demo with the same env var.
- **Auditable.** The startup `WARN` log records that the flag was active.
- **CI-ready.** Future CI jobs can run promptfoo with and without the flag and
  enforce that each scenario produces the expected pass/fail pattern.

The flag wiring lives in
[`src/main/java/com/example/springailab/config/ChatClientConfig.java`](../src/main/java/com/example/springailab/config/ChatClientConfig.java).

## Prerequisites

1. Spring AI is running with the **memory advisor enabled** (default):
   ```bash
   ./start-demo.sh   # from repo root
   ```
2. Promptfoo prerequisites: Node 20+, the judge model `llama3.1:8b` pulled in
   Ollama, and `promptfooconfig.yaml` pointing the judge at it.
3. Optional but recommended: `jq` installed (`brew install jq`) so the script
   can print pass/fail counts in its summary.

## Run the demo

From the `promptfoo/` directory:

```bash
./regression-demo.sh
```

The script runs three promptfoo evaluations in sequence and pauses between
them for you to restart Spring AI with the appropriate flag.

```
Phase 1 — Baseline (memory enabled)
        → expects 4/4 PASS
        → saves report/regression-<ts>/01-baseline.{html,json}

[PAUSE]   Operator action:
          Stop start-demo.sh, relaunch with the flag:
              APP_DEMO_MEMORY_DISABLED=true ./start-demo.sh

Phase 2 — Broken (memory disabled)
        → expects multi-turn checks to FAIL
        → saves report/regression-<ts>/02-broken.{html,json}

[PAUSE]   Operator action:
          Stop start-demo.sh, relaunch normally:
              ./start-demo.sh

Phase 3 — Restored (memory enabled again)
        → expects 4/4 PASS
        → saves report/regression-<ts>/03-restored.{html,json}

Summary printed at the end.
```

## What to highlight in the reports

Open the three HTML reports side-by-side in a browser. For each multi-turn
checking row (`memory[A] check`, `memory[B] check`):

| Report | Verdict | What the SUT response looks like |
|---|---|---|
| `01-baseline.html` | **PASS** | *"Your name is Alice."* — confident recall |
| `02-broken.html` | **FAIL** | *"I don't have any information about you, this conversation just started"* |
| `03-restored.html` | **PASS** | *"Your name is Alice."* again |

The SUT is unchanged at the model level — the only difference is the absence
of the advisor. Promptfoo distinguishes *technical health* (the endpoint
responds 200 in all three phases) from *behavioral correctness* (the response
is right). A liveness check would not have caught this regression.

The judge's reasoning, surfaced inline in the HTML report, is what
differentiates this from a unit test that would just say "expected X, got Y".
A non-engineer reading the broken-state report can immediately see *why* it
failed — useful for a regulated audit trail.

## Live presentation script (suggested 90 seconds)

> *"Here are two test cases — Alice introduces herself in turn 1 and asks her
> name in turn 2; the second case sets dietary constraints and asks for a
> lunch idea. We have a single command — `./regression-demo.sh` — that runs
> three rounds in a row. The first round is the current build, all green."*
> **[show 01-baseline.html]**
>
> *"Now I disable the chat-memory advisor — not by editing code, but by
> setting an environment variable that the app already supports. This is the
> kind of three-line change someone could make while cleaning up a config.
> The app still starts, the endpoint still returns 200."*
> **[restart Spring with the flag, press Enter]**
>
> *"Same tests, same model, same input. Watch the broken report."*
> **[show 02-broken.html, click into the Alice case, read the judge's
> reasoning aloud]**
>
> *"Restore the flag, restart, run the third round."*
> **[show 03-restored.html, all green again]**
>
> *"Without promptfoo this regression would only surface when a real user
> tried to have a multi-turn conversation. With promptfoo it surfaces on
> the first PR run."*

## Why this is the headline demo

| Property | Why it matters for the AI Team |
|---|---|
| Realistic regression | Mirrors a class of bug that has shipped in many production AI products |
| Zero new test cases needed | The existing `multi_turn.yaml` carries the regression already |
| Visible in seconds | One script, three reports, one human-readable reason per failure |
| Promptfoo-specific advantage | The judge's reasoning is what differentiates this from a unit test that would just say "expected X, got Y" |
| Translates to the psychotherapy use case | Conversational memory is mission-critical there; a regression of this kind is a P0 safety event |

## Safety notes

- **Never set `APP_DEMO_MEMORY_DISABLED=true` in any production-like
  environment.** The startup `WARN` log will scream if you do; treat it as a
  tripwire.
- **Don't commit a `.env` or run-script** that hardcodes the flag to `true`.
  The flag is for demo and CI scenarios only.
- **Restart hygiene.** Forgetting to restart Spring after restoring the flag
  will leave the audience confused — always validate with the final green
  Phase 3 run before declaring the demo over. The script does this for you.

## Roadmap — eliminating the manual restarts

A future iteration can remove the two operator-pause moments by exposing the
memory toggle as an HTTP header or query parameter (rather than a process-wide
env var). Promptfoo would then declare two providers in the same config — one
with memory, one without — and produce a single side-by-side report in one
run, no restarts. Tracked in
[`docs/promptfoo-analysis.md`](../docs/promptfoo-analysis.md) as a P2
enhancement; out of scope for this POC.
