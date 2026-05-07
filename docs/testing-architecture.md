# Testing Architecture — High-level

Four independent test suites converge on the same Spring Boot SUT
(`/ai/generate?message=...&chatId=...`), each with a different role:

- **DeepEval** & **Promptfoo** — offline quality regression (LLM-as-judge).
- **Langfuse** — runtime observability + evaluation on traces.
- **Garak** — adversarial / safety probing.

The judge model is the same local Ollama instance the SUT uses, kept
local for offline reproducibility. The SUT itself is unchanged across
suites — every suite is just an HTTP client.

## Mermaid

```mermaid
flowchart LR
  subgraph DEV["Local dev box"]
    direction LR

    subgraph SUT["spring-ai-lab (system under test)"]
      EP["GET /ai/generate?message=&chatId=<br/>(Basic auth, JSON reply)"]
      MEM[("Redis<br/>chat memory<br/>keyed by chatId")]
      EP --- MEM
    end

    subgraph OLLAMA["Ollama (local)"]
      SUT_MODEL["SUT model<br/>llama3.2:1b"]
      JUDGE_MODEL["judge model<br/>llama3.2:1b<br/>(override to 7B+)"]
    end
    EP -- "chat completion" --> SUT_MODEL

    subgraph DEEPEVAL["deepeval/  (pytest)"]
      DE_TEST["LLMTestCase + metrics<br/>(Relevancy, Faithfulness,<br/>GEval, Memory)"]
    end
    DE_TEST -- "HTTP" --> EP
    DE_TEST -- "judge calls" --> JUDGE_MODEL

    subgraph PROMPTFOO["promptfoo/  (yaml)"]
      PF_TEST["promptfooconfig.yaml<br/>http provider +<br/>llm-rubric assertions"]
    end
    PF_TEST -- "HTTP" --> EP
    PF_TEST -- "judge via /v1" --> JUDGE_MODEL

    subgraph LANGFUSE["langfuse/  (pytest + SDK)"]
      LF_TEST["instrumented pytest<br/>start_as_current_span +<br/>score_current_trace"]
      LF_UI["Langfuse self-host<br/>(:3000)<br/>traces, sessions, scores"]
      LF_TEST -- "spans + scores" --> LF_UI
    end
    LF_TEST -- "HTTP" --> EP
    LF_TEST -- "ollama_judge.py" --> JUDGE_MODEL

    subgraph GARAK["garak/  (CLI + REST gen)"]
      GK_RUN["garak --model_type rest<br/>probes: promptinject, dan,<br/>encoding, lmrc, goodside"]
      GK_REPORT["HTML + JSONL report"]
      GK_RUN --> GK_REPORT
    end
    GK_RUN -- "HTTP (probes as $INPUT)" --> EP

  end

  classDef sut fill:#dbeafe,stroke:#1e40af,color:#0c1f4a
  classDef ollama fill:#fef3c7,stroke:#92400e,color:#3b1d04
  classDef suite fill:#ecfccb,stroke:#3f6212,color:#1a2e05
  class SUT sut
  class OLLAMA ollama
  class DEEPEVAL,PROMPTFOO,LANGFUSE,GARAK suite
```

## ASCII (for terminals / PRs without Mermaid)

```
                          ┌─────────────────────────────────────────┐
                          │  Ollama (local, :11434)                 │
                          │   ┌──────────────┐  ┌──────────────┐    │
                          │   │ SUT model    │  │ judge model  │    │
                          │   │ llama3.2:1b  │  │ (same or 7B+)│    │
                          │   └──────┬───────┘  └──────┬───────┘    │
                          └──────────┼─────────────────┼────────────┘
                                     │                 │
                                     ▼                 │ judge calls
                          ┌─────────────────────┐      │ (relevancy,
                          │  spring-ai-lab      │      │  faithfulness,
                          │                     │      │  rubric)
                          │  GET /ai/generate   │      │
                          │   ?message=…        │      │
                          │   &chatId=…         │      │
                          │  (Basic auth,       │      │
                          │   JSON reply)       │      │
                          │         │           │      │
                          │         ▼           │      │
                          │  Redis chat memory  │      │
                          │  (keyed by chatId)  │      │
                          └─────────┬───────────┘      │
                                    │                  │
            ┌─────────────┬─────────┴────────┬────────────┬───────────┐
            │             │                  │            │           │
            ▼             ▼                  ▼            ▼           │
      ┌──────────┐  ┌──────────────┐  ┌─────────────┐  ┌────────┐    │
      │ deepeval │  │  promptfoo   │  │  langfuse   │  │ garak  │    │
      │ (pytest) │  │   (yaml)     │  │ (pytest +   │  │ (CLI + │    │
      │          │  │              │  │  SDK)       │  │  REST  │    │
      │ goldens  │  │ promptfoo-   │  │ traces,     │  │  gen)  │    │
      │ +        │  │ config.yaml  │  │ sessions,   │  │ probes,│    │
      │ metrics  │  │ + llm-rubric │  │ online evals│  │ jail-  │    │
      │          │  │              │  │             │  │ breaks │    │
      └──────────┘  └──────────────┘  └──────┬──────┘  └───┬────┘    │
                                             │             │         │
                                             ▼             ▼         │
                                      ┌─────────────┐ ┌─────────┐    │
                                      │ Langfuse UI │ │ HTML +  │    │
                                      │  (:3000)    │ │ JSONL   │    │
                                      │  traces,    │ │ report  │    │
                                      │  scores     │ │         │    │
                                      └─────────────┘ └─────────┘    │
                                                                     │
            ┌────────────────────────────────────────────────────────┘
            │  All four suites are independent HTTP clients of the
            │  SAME SUT. They differ in:
            │   • where the test cases live (Python / YAML / probe
            │     corpus / dataset in Langfuse)
            │   • who scores the response (built-in metrics vs.
            │     llm-rubric vs. detector classifier)
            │   • where results land (pytest exit code / HTML / UI)
            └────────────────────────────────────────────────────────
```

## Pipeline placement

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ Local dev   │  │ PR / CI     │  │ Pre-release │  │ Production  │
└─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘
      │                │                │                │
   Promptfoo         DeepEval         Garak            Langfuse
 (YAML iter,       (pytest gate     (security       (live obs +
  fast feedback)    on PR)           scan)           online eval
                                                     on real users)
```

This is the recommended split — see
[`ai-testing-stack-analysis.md`](ai-testing-stack-analysis.md) for the
full rationale.