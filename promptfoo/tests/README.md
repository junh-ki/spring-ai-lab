# Test categories

Each file in this directory contains test cases for one **category of model
behavior**. The filename names what the cases verify, not how they are run.
Add new cases to the matching file; add a new file only when introducing a
genuinely new category.

| File | What it verifies | Stateless? | Run hint |
|---|---|---|---|
| [`functional.yaml`](functional.yaml) | Happy-path correctness on standard questions (factual, arithmetic, tone, length). | yes | any concurrency |
| [`refusal.yaml`](refusal.yaml) | The model must decline to fabricate information it does not have (PII, medical / legal). | yes | any concurrency |
| [`safety.yaml`](safety.yaml) | Model behavior in sensitive scenarios — crisis handling, self-harm, abuse disclosure. Critical for the psychotherapy use case. | yes | any concurrency |
| [`memory-regression.yaml`](memory-regression.yaml) | Memory through multi-turn conversations — does the model recall facts and apply preferences from earlier turns sharing the same `chatId`? | **no — stateful** | **`-j 1` required** |

## How to run

```bash
./run.sh                                  # all enabled categories
./run.sh --filter-pattern memory          # only memory-regression
./run.sh --filter-pattern factual         # one row (description match)
./run.sh --no-cache                       # bypass the cache (mandatory for stateful tests)
```

For the full regression demo (Demo 1 of the POC), use the orchestrator:

```bash
./regression-demo.sh
```

## Adversarial / red-team

Adversarial scanning is **delegated to Garak** (a separate NVIDIA Research POC
in this repo) — see [`../poc.md` §7.2](../poc.md) for the rationale. Promptfoo's
`redteam` feature exists and is competitive on plugin catalogue, but its
accountability gating and remote-default attack generation are incompatible
with the offline-first, clinically-regulated posture of this POC.

## Adding a new category

1. Create `tests/<category>.yaml` with a header comment explaining what the
   file verifies (one paragraph).
2. Add the file to `promptfooconfig.yaml` under `tests:`.
3. Update the table in this README.
4. If the category requires special run flags (like memory-regression needs
   `-j 1`), call that out explicitly.
