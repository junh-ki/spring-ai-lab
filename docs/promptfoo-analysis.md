# Promptfoo — Best-Practices Gap Analysis

A QA-oriented review of the current `promptfoo/` setup against promptfoo best practices and what a production-grade evaluation suite typically looks like.

**Scope:** static analysis of [`promptfooconfig.yaml`](../promptfoo/promptfooconfig.yaml), [`tests/*.yaml`](../promptfoo/tests), and [`run.sh`](../promptfoo/run.sh). No code was changed; this is a recommendations document.

---

## 1. Current state — at a glance

| Area | Status |
|---|---|
| Provider (HTTP + Basic auth + URL templating) | ✅ correct and well-commented |
| Default judge configured (`defaultTest.options.provider`) | ✅ |
| Tests split by category (single vs multi-turn) | ✅ |
| Mix of deterministic + LLM-as-judge assertions | ✅ |
| Pre-flight SUT health check in `run.sh` | ✅ |
| HTML + JSON report output | ✅ |
| CI integration | ❌ missing |
| Adversarial / red-team coverage | ❌ missing |
| Strong / ensemble judge | ⚠️ single 1B local judge (acknowledged as noisy) |
| Auth credentials hardcoded in YAML | ⚠️ security gap |
| `defaultTest.assert` for global thresholds | ❌ missing |
| Tagging / metadata for filtering | ❌ missing |
| Provider matrix (multi-model comparison) | ❌ missing — promptfoo's signature feature unused |
| Assertion variety (`is-json`, `factuality`, `regex`, JS) | ⚠️ only `contains`, `llm-rubric`, `latency` |
| Determinism controls (seed, temperature pinning) | ❌ missing |
| Report archiving (overwrites `last.*`) | ⚠️ no history |

The suite is **a solid POC** — the architectural choices (HTTP provider, OpenAI-compat for the Ollama judge, file-split tests, pre-flight check) are textbook. The gaps are mostly about **scaling beyond a POC**: making it CI-enforceable, adversarially robust, and reproducible.

---

## 2. P0 — must address for a professional QA setup

### 2.1 Move credentials out of the YAML

**Current:**

```yaml
headers:
  Authorization: 'Basic dXNlcjpkZW1v'  # base64 of "user:demo"
```

**Why it matters:** even though the password is `demo` for the local profile, hardcoding base64-encoded credentials in a checked-in file is a habit that breaks the moment someone points the suite at a staging/prod URL. `run.sh` already exports `SPRING_USER` and `SPRING_PASSWORD` — but the config does not consume them.

**Fix:** use promptfoo's environment-variable interpolation in the provider config, and Basic-auth via username/password fields rather than a pre-encoded header. Reference: [http provider docs](https://www.promptfoo.dev/docs/providers/http/#basic-authentication).

```yaml
providers:
  - id: spring-ai-lab
    config:
      url: '{{ env.SPRING_BASE_URL }}/ai/generate?message={{prompt | urlencode}}&chatId={{chatId}}'
      method: GET
      headers:
        Authorization: 'Basic {{ env.SPRING_BASIC_AUTH_B64 }}'
      transformResponse: 'json.generation'
      timeoutMs: 180000
```

…and have `run.sh` compute the header once:

```bash
export SPRING_BASIC_AUTH_B64="$(printf '%s:%s' "$SPRING_USER" "$SPRING_PASSWORD" | base64)"
```

### 2.2 Wire promptfoo into CI with a pass-rate gate

**Current:** the suite only runs locally. There is no automated regression catch.

**Why it matters:** promptfoo is most valuable when it runs on every PR and blocks merges below a quality threshold. Otherwise it is a manual checklist.

**Fix:** add a GitHub Actions workflow that boots the SUT (or a mocked SUT), runs `promptfoo eval`, and uses `--fail-on` (or parses `report/last.json`) to enforce a pass-rate threshold. Promptfoo also publishes a [first-party action](https://www.promptfoo.dev/docs/integrations/github-action/) that can post the diff vs. main as a PR comment.

```yaml
# .github/workflows/promptfoo.yml (sketch)
- name: Run promptfoo
  working-directory: promptfoo
  run: |
    npx promptfoo@latest eval \
      --config promptfooconfig.yaml \
      --output report/last.json \
      --no-progress-bar
- name: Enforce pass rate
  run: |
    npx promptfoo@latest eval --share=false \
      --assert-pass-rate 0.9 \
      --config promptfoo/promptfooconfig.yaml
```

> Verify the exact flag name against your installed version (`promptfoo eval --help`); recent versions prefer `derivedMetrics` + JSON post-processing for thresholding.

### 2.3 Add adversarial / red-team coverage

**Current:** one ad-hoc refusal test (`What is my home address?`).

**Why it matters:** promptfoo ships a [`redteam` subcommand](https://www.promptfoo.dev/docs/red-team/) that auto-generates **hundreds of adversarial test cases** for harmful content, prompt injection, jailbreaks, PII leakage, and hallucination. For a QA deliverable this is the single highest-leverage missing feature — it converts the suite from "happy-path checks" to "robust safety regression."

**Fix:** add a `redteam:` section to `promptfooconfig.yaml` (it is a top-level section of the existing config, not a separate file) and run the dedicated subcommand alongside the functional suite. The repo's [`testing-architecture.md`](testing-architecture.md) currently delegates adversarial scanning to Garak; promptfoo's red team is a complementary in-suite layer (cheaper to run, integrated into the same report).

```bash
cd promptfoo
npx promptfoo@latest redteam init --no-gui    # adds a redteam: section to promptfooconfig.yaml
# edit promptfooconfig.yaml: set purpose, pick plugins, point the attack
# generation provider at the local Ollama judge to stay offline
npx promptfoo@latest redteam generate         # auto-creates adversarial test cases
npx promptfoo@latest redteam run              # executes them against the SUT
```

Recommended plugins to enable for a chat assistant in a clinical-adjacent domain: `harmful:self-harm`, `harmful:specialized-advice`, `harmful:misinformation-disinformation`, `pii:direct`, `hallucination`, `prompt-extraction`, `excessive-agency`, `imitation`. The output is the same HTML report shape — easy to attach to your POC presentation.

### 2.4 Strengthen or ensemble the judge

**Current:** single `llama3.2:1b` judge, acknowledged as noisy in the README.

**Why it matters:** an unreliable judge produces both false positives (passes broken behavior) and false negatives (fails working behavior). For a deliverable, "we couldn't trust the score" is the worst possible outcome.

**Theoretical foundation.** The seminal paper on LLM-as-a-Judge — [Zheng et al., "Judging LLM-as-a-Judge with MT-Bench and Chatbot Arena"](https://arxiv.org/abs/2306.05685) (NeurIPS 2023) — establishes that a *strong* LLM judge can match human-vs-human agreement (>80%) on open-ended evaluation. Their concrete proof point in 2023 was GPT-4; the principle generalizes to "use the strongest model your operational constraints allow". The paper also catalogues judge biases (position, verbosity, self-enhancement) that should be mitigated regardless of model size.

**The frontier-API vs local trade-off.** A frontier judge (Claude Opus, GPT-5, Gemini Ultra) is closer to the paper's GPT-4 reference than any local model — but introduces three constraints that matter for this project:

- **Privacy.** Sending test prompts and responses to an external API means clinical-adjacent data leaves your infrastructure. For a regulated domain (psychotherapy, healthcare, legal), this is often a non-starter regardless of judge quality.
- **Cost.** Frontier-model API calls accumulate quickly. A judge run over a few hundred test cases with multiple rubrics each can move from cents to tens of dollars per run.
- **Reproducibility.** API providers update their models silently; a six-month-old reference run can drift from a current run because the judge changed underneath, not the SUT.

For this POC, **a local 8B judge is a deliberate choice, not a fallback.** It preserves offline reproducibility and keeps clinical-flavored data inside the lab. The trade-off (less reasoning capacity than a frontier judge) is mitigated by the deterministic-first assertion design and by the items below.

**Why the asymmetry is correct (judge > SUT).** A common objection is *"shouldn't both sides use the same model?"* The answer is no — and the asymmetry is intentional:

- **Production economics differ.** The SUT (1B) reflects production reality: cheap, low-latency, scalable to thousands of QPS. The judge runs offline over a bounded test set; cost-per-call is irrelevant next to quality-of-judgment.
- **A weaker judge cannot see SUT errors.** If the judge is less capable than the SUT, it cannot reliably recognize when the SUT has made a subtle mistake, and the eval becomes useless. Judge ≥ SUT is a necessary condition for the rubric to do its job.
- **Industry and literature precedent.** This mirrors human QA (senior reviewing junior) and the original LLM-as-Judge experiments ([Zheng et al., 2023](https://arxiv.org/abs/2306.05685)), which used Vicuna / LLaMA-7B / 13B as SUTs and GPT-4 as judge — judge ≫ SUT.

Talking-point version for the presentation: *"The production model has to be cheap and fast for thousands of users. The judge runs offline on a test set, so we can afford to give it 8× more parameters. Technically we want the judge to be more capable than the SUT — otherwise it cannot recognize the SUT's errors."*

**Fix — pick at least one:**

1. **Default to a 7B+ local judge.** Pull `llama3.1:8b` or `qwen2.5:7b` in `start-demo.sh` and update `defaultTest.options.provider.id`. Document the memory cost. The cheapest reliability win for a local-first eval.
2. **Pin judge temperature and other generation knobs** so scores are reproducible run-to-run:
   ```yaml
   defaultTest:
     options:
       provider:
         id: openai:chat:llama3.1:8b
         config:
           apiKey: ollama
           apiBaseUrl: http://127.0.0.1:11434/v1
           temperature: 0
           seed: 42
   ```
3. **Use multiple graders for the same assertion** (a "judge ensemble") on critical cases, agreeing only when k-of-n pass. Promptfoo supports this via the `provider` array on `llm-rubric`. This is the closest local mitigation to the frontier-judge gap.
4. **Future work — periodic frontier calibration.** For production deployment in a regulated domain, consider running a sampled subset of cases against a frontier API judge on anonymized data to detect drift in the local judge's calibration over time. Out of scope for the POC; worth registering as a roadmap item.

---

## 3. P1 — substantial improvements

### 3.1 Add `defaultTest.assert` for repo-wide thresholds

Today every test must opt into a latency cap. A `defaultTest.assert` block applies assertions to **every** row, and per-row asserts add to (not replace) the defaults:

```yaml
defaultTest:
  assert:
    - type: latency
      threshold: 90000
    - type: cost
      threshold: 0.01          # only meaningful for paid providers
    - type: javascript
      value: output.length > 0 && output.length < 4000
  options:
    provider: { ... }
```

This makes "no empty replies" and "no infinite latency" structural rather than per-test.

### 3.2 Tag tests for selective runs

Add a `metadata` block to each test:

```yaml
- description: factual — capital of France
  metadata:
    category: factual
    risk: low
  vars: { ... }
```

Then filter via `--filter-metadata category=factual` in CI smoke runs vs. nightly full runs. Lets you have a fast PR gate (factual + memory only) and a slow nightly gate (everything including red-team).

### 3.3 Diversify assertion types

The current suite uses three assertion types. Promptfoo supports many more — picking the right one reduces dependence on the noisy judge:

| Use case | Right assertion | Why over `llm-rubric` |
|---|---|---|
| Reply must be valid JSON | `is-json` | deterministic, free |
| Reply must match a structural pattern | `regex` | deterministic, free |
| Custom logic over the response object | `javascript` / `python` | deterministic, free |
| Faithfulness to a reference doc (RAG) | `factuality` (built-in rubric) | calibrated rubric |
| Topical relevance | `answer-relevance` | calibrated rubric |
| Refuse-to-answer detection | `classifier` (HuggingFace) | dedicated model |

Example — replace the arithmetic `llm-rubric` with `regex`:

```yaml
- description: arithmetic — small sum
  vars: { prompt: 'What is 7 plus 5? Answer with just the number.', chatId: pf-single-002 }
  assert:
    - type: regex
      value: '\b12\b'
```

### 3.4 Use a provider matrix to compare models

> **Status in this POC:** documented capability, **not demonstrated**. The primary goal of this POC was regression detection (Demo 1), which needs a single provider. Multi-provider comparison is left as a deliberate scope decision; the configuration shown below is the entire delta required to enable it later.

The single biggest selling point of promptfoo over plain pytest is **side-by-side model comparison on the same test cases**. When the team is ready to choose between candidate models for production, this answers *"which model should we deploy?"* in one run, with one report. The repo already supports OpenAI and Bedrock provider profiles ([`README.md` provider profiles](../README.md#provider-profiles)).

Adding a second provider is a one-line addition; the operational setup is the longer step (Spring AI must be booted twice, on different ports, with different `APP_CHAT_MODEL` values):

```yaml
providers:
  - id: spring-with-llama-1b
    label: 'candidate A — llama3.2:1b'
    config:
      url: 'http://localhost:8080/ai/generate?message={{prompt | urlencode}}&chatId={{chatId}}-a'
      # ...
  - id: spring-with-llama-3b
    label: 'candidate B — llama3.2:3b'
    config:
      url: 'http://localhost:8081/ai/generate?message={{prompt | urlencode}}&chatId={{chatId}}-b'
      # ...
```

Run promptfoo against the resulting config and the HTML report shows pass rate **per provider per case** — exactly the artifact a model-selection review board needs. Total setup time: ~1 hour (mostly booting Spring twice with different profiles).

This is the right next demonstration the moment the team's question shifts from *"does promptfoo work?"* (answered by this POC) to *"which model do we ship?"*.

### 3.5 Force serial execution for memory tests

The multi-turn file assumes rows run in declared order. With `-j > 1`, two priming/checking pairs in different conversations could interleave incorrectly. Either:

- pin `-j 1` in `run.sh` for all runs (simplest, slower), or
- split memory tests into a separate config and run that one with `-j 1`, while letting single-turn parallelize.

### 3.6 Archive reports

`report/last.html` and `report/last.json` are overwritten on every run, so you cannot diff "today vs last week". Tag with a timestamp:

```bash
ts="$(date -u +%Y%m%dT%H%M%SZ)"
npx --yes promptfoo@latest eval \
  --config promptfooconfig.yaml \
  --output "report/${ts}.html" \
  --output "report/${ts}.json"
ln -sf "${ts}.html" report/last.html
ln -sf "${ts}.json" report/last.json
```

Or use `promptfoo share` to push to the cloud dashboard (verify policy first — this uploads test data).

---

## 4. P2 — nice-to-have

- **CSV-driven datasets.** Promptfoo accepts `tests: file://cases.csv`. For non-engineers maintaining cases, a spreadsheet is friendlier than YAML. Keep `memory-regression.yaml` in YAML (CSV doesn't express ordering well), migrate the stateless files (`functional.yaml`, `refusal.yaml`, `safety.yaml`) if QA wants spreadsheet ergonomics.
- **Explicit cache configuration.** Promptfoo caches both SUT and judge calls by default. Document this in the README and add `--no-cache` to CI runs that should be deterministic from cold.
- **Cost-aware assertions.** Once you wire OpenAI/Bedrock providers in (3.4), add `cost` assertions to keep eval bills bounded.
- **Scenarios block** — DRY for shared variable sets across tests:
  ```yaml
  scenarios:
    - description: vegetarian baseline
      config:
        - vars: { user_pref: 'vegetarian, allergic to peanuts' }
      tests:
        - description: lunch idea
          # ...
  ```
- **Custom JavaScript hooks** (`extensions:`) for response normalization or trace capture into Langfuse. Out of scope for now; document as a future bridge.

---

## 5. Prioritized checklist

| # | Item | Priority | Effort |
|---|---|---|---|
| 2.1 | Env-var auth, no credentials in YAML | P0 | S |
| 2.2 | CI workflow with pass-rate gate | P0 | M |
| 2.3 | Red-team / adversarial layer | P0 | M |
| 2.4 | Stronger / pinned judge | P0 | S |
| 3.1 | `defaultTest.assert` thresholds | P1 | S |
| 3.2 | Tagged tests + metadata filter | P1 | S |
| 3.3 | Diversify assertion types | P1 | M |
| 3.4 | Multi-provider matrix | P1 | M |
| 3.5 | Force `-j 1` for memory tests | P1 | XS |
| 3.6 | Archived reports with timestamps | P1 | S |
| 4.x | CSV datasets, scenarios, cost asserts | P2 | varies |

Effort: XS = <30 min, S = a morning, M = a day or two, L = multi-day.

---

## 6. Learning resources — what to read, in what order

Read these in order; each builds on the previous one.

1. **Getting started + core concepts** — [promptfoo.dev/docs/getting-started](https://www.promptfoo.dev/docs/getting-started). 15 min.
2. **Configuration reference** — [promptfoo.dev/docs/configuration/reference](https://www.promptfoo.dev/docs/configuration/reference). The single most useful page; bookmark it.
3. **Assertion types catalog** — [promptfoo.dev/docs/configuration/expected-outputs](https://www.promptfoo.dev/docs/configuration/expected-outputs). Skim the index, deep-read `llm-rubric`, `factuality`, `answer-relevance`, `is-json`, `javascript`.
4. **HTTP provider** — [promptfoo.dev/docs/providers/http](https://www.promptfoo.dev/docs/providers/http). Specifically the templating, transforms, and auth subsections.
5. **Red team** — [promptfoo.dev/docs/red-team](https://www.promptfoo.dev/docs/red-team). Read the plugin index; pick the ones that match your app's risk surface.
6. **CI/CD integration** — [promptfoo.dev/docs/integrations/ci-cd](https://www.promptfoo.dev/docs/integrations/ci-cd) and the [GitHub Action](https://www.promptfoo.dev/docs/integrations/github-action).
7. **Sharing & dashboards** — [promptfoo.dev/docs/usage/sharing](https://www.promptfoo.dev/docs/usage/sharing). Understand what gets uploaded before enabling it on confidential prompts.
8. **Datasets & scenarios** — [promptfoo.dev/docs/configuration/test-cases](https://www.promptfoo.dev/docs/configuration/test-cases). For when the test list grows past a few dozen rows.

For broader context on LLM evaluation as a discipline (independent of promptfoo): the OpenAI Evals repo, the Stanford HELM benchmark methodology paper, and Anthropic's published model card evaluation appendices are the three references most commonly cited by AI QA teams.

---

## 7. Summary

The current suite is a clean, idiomatic promptfoo POC — the architecture is right, the inline documentation is exceptionally good. The work to make it a **professional QA artifact** is concentrated in four areas: **CI enforcement, adversarial coverage, judge reliability, and credential hygiene**. Everything else (assertion variety, tagging, multi-provider) is incremental improvement on top of those four.

If only one change were possible: **add the red-team layer (§2.3)**. It is the feature with the highest delta-utility per hour invested, and it is what differentiates promptfoo from a pytest harness in the eyes of a security-aware reviewer.

---

## 8. Appendix — Promptfoo vs DeepEval capability matrix

Binary positioning of the two tools across the capabilities that matter for a regulated / high-stakes AI product. ✓ = better fit, ✗ = weaker fit.

| # | Capability | DeepEval | Promptfoo |
|---|---|:---:|:---:|
| | **Regression & change management** | | |
| 1 | Regression test suite (PR gate) | ✗ | ✓ |
| 2 | Cross-run diff (which cases changed verdict) | ✗ | ✓ |
| 3 | Adding a one-line case for a new bug | ✗ | ✓ |
| 4 | Tag / metadata filtering at CLI | ✗ | ✓ |
| 5 | Result caching across runs | ✗ | ✓ |
| 6 | Multi-provider comparison matrix (model A vs B on same cases) | ✗ | ✓ |
| | **Safety & adversarial** | | |
| 7 | Red-team / jailbreak generation | ✗ | ✓ |
| 8 | Prompt-injection probing | ✗ | ✓ |
| 9 | Refusal-calibration coverage at scale | ✗ | ✓ |
| | **Authorship & accessibility** | | |
| 10 | PM / QA / clinician authoring without engineer | ✗ | ✓ |
| 11 | Reviewable in plain PR diff | ✗ | ✓ |
| 12 | Cross-language CI (no Python toolchain required) | ✗ | ✓ |
| | **Reporting & stakeholder review** | | |
| 13 | Audit / regulatory HTML artifact | ✗ | ✓ |
| 14 | Shareable link for external reviewer | ✗ | ✓ |
| 15 | Per-case judge reasoning visible in report | ✗ | ✓ |
| 16 | Historical run dashboard with persistent storage and two-run diff (`promptfoo view`) | ✗ | ✓ |
| | **Assertion economics** | | |
| 17 | Deterministic free assertions (regex/contains/json/latency) | ✗ | ✓ |
| 18 | Memory regression — fast to author | ✗ | ✓ |
| | **RAG quality** | | |
| 19 | Faithfulness metric (calibrated) | ✓ | ✗ |
| 20 | Contextual Precision / Recall | ✓ | ✗ |
| 21 | Hallucination metric | ✓ | ✗ |
| 22 | Component-level scoring (retriever, reranker, generator) | ✓ | ✗ |
| | **Conversational / multi-turn rigor** | | |
| 23 | Conversation Completeness metric | ✓ | ✗ |
| 24 | Knowledge Retention metric | ✓ | ✗ |
| 25 | Memory regression — rigorous metric | ✓ | ✗ |
| | **Programmatic depth** | | |
| 26 | Custom metric as Python class | ✓ | ✗ |
| 27 | Domain-custom clinical / legal / financial metric | ✓ | ✗ |
| 28 | Pytest ecosystem (fixtures, parametrize, plugins) | ✓ | ✗ |
| 29 | Programmatic composition (DB-driven cases, loops) | ✓ | ✗ |
| 30 | Statistical / calibrated metric contract `(score, reason, threshold)` | ✓ | ✗ |
| | **Test data generation** | | |
| 31 | Synthetic Q/A pairs from internal docs (functional) | ✓ | ✗ |
| 32 | Synthetic adversarial cases (security) | ✗ | ✓ |

**Tally:** Promptfoo 18, DeepEval 14. The split is not "one is better" — they cover **different categories**. Promptfoo dominates the *operational* axis (regression, safety, accessibility, reporting, historical monitoring); DeepEval dominates the *scientific* axis (calibrated metrics, RAG decomposition, custom Python metrics). A mature evaluation stack uses both.
