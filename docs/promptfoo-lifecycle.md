# Promptfoo — Lifecycle and Component Diagram

> Reference artifact for the Promptfoo POC. Three complementary views of the
> end-to-end evaluation pipeline as it is wired in this repository:
>
> 1. **Topology** — every participating process and class, at a glance.
> 2. **Lifecycle** — time-ordered sequence from `./run.sh` to `report/last.html`.
> 3. **Control flow** — branching logic and assertion cost (where the eval
>    decides, and what each branch costs).
>
> All three diagrams reference the exact source files so a reviewer can
> navigate straight from the picture to the code.

---

## 1. Topology — what each box is and where it runs

```mermaid
flowchart TB
    subgraph DEV["Developer machine"]
        OP(["Operator (QA / Developer)"])
        SHELL["<b>run.sh</b> / <b>regression-demo.sh</b><br/><i>shell wrappers — env defaults,<br/>pre-flight, NO_PROXY for localhost</i>"]
    end

    subgraph PFRUN["Promptfoo runtime — Node.js (spawned by npx)"]
        CONFIG["<b>promptfooconfig.yaml</b><br/>+ <b>tests/*.yaml</b><br/><i>functional · refusal · safety<br/>memory-regression</i>"]
        EVAL["<b>Eval engine</b><br/><i>matrix build · row ordering · -j N</i>"]
        HTTPP["<b>HTTP provider</b><br/><i>Nunjucks URL templating · urlencode ·<br/>Basic auth header · transformResponse</i>"]
        ASSERT["<b>Assertion runner</b><br/><i>contains · regex · latency · llm-rubric</i>"]
        JUDGECLI["<b>OpenAI-compat client</b><br/><i>POST /v1/chat/completions to Ollama</i>"]
        REP[("<b>report/</b><br/>last.html · last.json")]
    end

    subgraph SUT["spring-ai-lab — Spring Boot :8080"]
        CTRL["<b>ChatController</b><br/><i>GET /ai/generate</i>"]
        SVC["<b>ChatService</b><br/><i>orchestrates prompt + advisor</i>"]
        CHATCLI["<b>ChatClient</b><br/>+ <b>MessageChatMemoryAdvisor</b>"]
    end

    REDIS[("<b>Redis :6379</b><br/><i>chat memory keyed by chatId</i>")]

    subgraph OLLAMA["Ollama runtime :11434"]
        SUTM["<b>SUT model</b><br/>llama3.2:1b<br/><i>via native /api/chat</i>"]
        JM["<b>Judge model</b><br/>llama3.1:8b<br/><i>via OpenAI-compat /v1</i>"]
    end

    OP --> SHELL
    SHELL -->|spawns via npx| EVAL
    CONFIG -. loaded once .-> EVAL
    EVAL --> HTTPP
    EVAL --> ASSERT
    HTTPP -->|HTTP GET + Basic auth| CTRL
    CTRL --> SVC
    SVC --> CHATCLI
    CHATCLI <-->|read / append history| REDIS
    CHATCLI -->|chat completion| SUTM
    ASSERT -. llm-rubric only .-> JUDGECLI
    JUDGECLI -->|score + reason| JM
    EVAL --> REP

    classDef pf fill:#e3f2fd,stroke:#1976d2,color:#0d47a1
    classDef sutc fill:#fff3e0,stroke:#ef6c00,color:#3e2723
    classDef oll fill:#f3e5f5,stroke:#7b1fa2,color:#311b92
    classDef store fill:#eeeeee,stroke:#616161,color:#212121
    classDef dev fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    class OP,SHELL dev
    class CONFIG,EVAL,HTTPP,ASSERT,JUDGECLI pf
    class CTRL,SVC,CHATCLI sutc
    class SUTM,JM oll
    class REDIS,REP store
```

### Reading the topology

| Group | What runs there | Where the code lives |
|---|---|---|
| **Developer machine** | Shell wrappers that set env defaults, do a pre-flight against the SUT, and spawn Promptfoo. | [`promptfoo/run.sh`](../promptfoo/run.sh) · [`promptfoo/regression-demo.sh`](../promptfoo/regression-demo.sh) |
| **Promptfoo runtime** | The Node.js process spawned by `npx`. Loads the YAML config, builds the test matrix, calls the SUT, runs assertions, writes the report. | [`promptfoo/promptfooconfig.yaml`](../promptfoo/promptfooconfig.yaml) · [`promptfoo/tests/*.yaml`](../promptfoo/tests/) |
| **spring-ai-lab** | The system under test. Stateless HTTP controller backed by a chat client with a memory advisor. | [`ChatController.java`](../src/main/java/com/example/springailab/chat/ChatController.java) · [`ChatService.java`](../src/main/java/com/example/springailab/chat/ChatService.java) · [`ChatClientConfig.java`](../src/main/java/com/example/springailab/config/ChatClientConfig.java) |
| **Redis** | Persists the per-`chatId` conversation history that `MessageChatMemoryAdvisor` injects on each turn. | `docker-compose.yml` |
| **Ollama** | Hosts both models on a single port. The same process serves the SUT (via the native `/api/chat` endpoint) and the judge (via the OpenAI-compatible `/v1` endpoint). | `docker-compose.yml` |

### LLM consumption summary

| Role | Model | Endpoint | Called by |
|---|---|---|---|
| **System under test** | `llama3.2:1b` | `POST /api/chat` (Ollama native) | Spring AI `ChatClient` |
| **Judge** (`llm-rubric` only) | `llama3.1:8b` | `POST /v1/chat/completions` (OpenAI-compatible) | Promptfoo assertion runner |

Both models live in the same Ollama process. The asymmetry — *judge stronger than SUT* — is intentional and documented in [`poc.md` §1](../promptfoo/poc.md#1-goal).

---

## 2. Lifecycle — end-to-end sequence

```mermaid
sequenceDiagram
    autonumber
    actor Op as Operator
    participant Sh as run.sh
    participant PF as promptfoo<br/>(via npx)
    participant Cfg as Config + tests
    box rgb(255, 243, 224) spring-ai-lab :8080
        participant Ctrl as ChatController
        participant Svc as ChatService
        participant Cli as ChatClient<br/>+ MemoryAdvisor
    end
    participant Mem as Redis :6379
    box rgb(243, 229, 245) Ollama :11434
        participant Sutm as llama3.2:1b<br/>(SUT)
        participant Jdg as llama3.1:8b<br/>(judge, /v1)
    end
    participant Out as report/<br/>last.{html,json}

    rect rgb(232, 244, 248)
    Note over Op,Out: Phase 1 — Bootstrap
    Op->>Sh: ./run.sh [--filter-pattern X | -j 1]
    Sh->>Sh: export NO_PROXY=localhost,127.0.0.1<br/>SPRING_* · OLLAMA_*
    Sh->>Ctrl: GET /ai/generate?message=ping (pre-flight)
    Ctrl-->>Sh: 200 OK
    Sh->>PF: npx --yes promptfoo@latest eval
    end

    rect rgb(232, 244, 248)
    Note over PF,Cfg: Phase 2 — Load + dereference
    PF->>Cfg: read promptfooconfig.yaml
    PF->>Cfg: dereference tests/*.yaml
    Cfg-->>PF: matrix [N rows × providers × asserts]
    end

    Note over PF,Out: Phase 3 — Evaluation loop
    loop for each test row (sequential when -j 1)
        PF->>PF: Nunjucks render — url, headers,<br/>{{prompt | urlencode}}, {{chatId}}
        PF->>Ctrl: HTTP GET /ai/generate?message=...&chatId=...<br/>Authorization: Basic dXNlcjpkZW1v
        activate Ctrl
        Ctrl->>Svc: generate(message, chatId)
        Svc->>Cli: prompt().user(message).advisors(chatId)
        Cli->>Mem: load history for chatId
        Mem-->>Cli: prior turns (may be empty)
        Cli->>Sutm: POST /api/chat<br/>messages = [history..., user]
        Sutm-->>Cli: generated text
        Cli->>Mem: append { user, assistant } turn
        Cli-->>Svc: String content
        Svc-->>Ctrl: String
        Ctrl-->>PF: {"generation":"..."}
        deactivate Ctrl
        PF->>PF: transformResponse → output string

        loop for each assertion in row
            alt deterministic (contains · regex · latency)
                PF->>PF: evaluate locally — no LLM call
            else llm-rubric
                PF->>Jdg: POST /v1/chat/completions<br/>{ rubric, input, output }
                Jdg-->>PF: { pass, score, reason }
            end
        end
        PF->>PF: aggregate row verdict
    end

    rect rgb(232, 244, 248)
    Note over PF,Out: Phase 4 — Report
    PF->>Out: write last.html + last.json
    PF-->>Op: stdout summary · exit code (0 = all pass)
    end
```

### Reading the lifecycle

**Phase 1 — Bootstrap.** The shell wrapper exists for one reason: setting the
environment that Promptfoo needs to work in a corporate environment. `NO_PROXY`
is the critical bit — without it, `undici` (Node's HTTP client) routes every
request through `HTTPS_PROXY` and times out trying to reach `localhost` via
the corporate proxy. The pre-flight curl gives a fast, readable failure when
the SUT is not up.

**Phase 2 — Load + dereference.** Each `file://tests/<name>.yaml` import is
expanded into actual row objects in memory. The matrix that comes out is
`providers × rows × asserts`. Only one provider is configured today; multi-
provider comparison is a future demo ([`poc.md` Demo 4](../promptfoo/poc.md#demo-4--multi-provider-comparison-advantage)).

**Phase 3 — The eval loop.** This is the work. Each row is one fully
independent invocation of the SUT plus its assertions, except for memory-
regression rows that share a `chatId` and therefore *depend on row ordering*
— which is why those tests demand `-j 1` (serial).

Within a row, two distinct LLM hops can happen:

| Hop | When | Cost |
|---|---|---|
| SUT call (`llama3.2:1b`) | Always — once per row | One Ollama call |
| Judge call (`llama3.1:8b`) | Only for `llm-rubric` / `factuality` assertions | One Ollama call per such assertion |

Deterministic assertions (`contains`, `regex`, `latency`, `is-json`) execute
locally in Node with zero LLM cost. A suite designed to use deterministic
assertions wherever possible is **orders of magnitude faster and cheaper**
than one that relies on `llm-rubric` for everything.

**Phase 4 — Report.** Two artifacts: `last.html` (human readable, includes the
judge's reasoning per `llm-rubric` row) and `last.json` (machine readable,
consumed by `regression-demo.sh` and any CI pipeline). The exit code is
non-zero if any assertion failed — the standard CI gate contract.

---

## 3. Control flow — branching logic and assertion cost

The topology and sequence diagrams above describe **who** runs **where** and
**when**. This third view answers a different question: **where does the eval
branch, and what does each branch cost?** Useful for arguing about CI
economics, suite design, and the CI gate contract.

```mermaid
flowchart TD
    Start([./run.sh starts]) --> PreCheck{SUT alive?<br/>HTTP 200?}
    PreCheck -->|No| Abort([Abort — SUT not reachable])
    PreCheck -->|Yes| Load[Load promptfooconfig.yaml<br/>+ all tests/*.yaml<br/>= N test rows]
    Load --> Rows[/Begin row loop/]
    Rows --> RenderURL[Nunjucks render:<br/>substitute prompt and chatId<br/>into provider URL]
    RenderURL --> HitSUT[HTTP GET /ai/generate<br/>Basic auth + URL params]
    HitSUT --> SUTOK{HTTP 200?}
    SUTOK -->|No| RowError[Mark row as error]
    SUTOK -->|Yes| Extract[Extract output:<br/>transformResponse = json.generation]
    Extract --> Assert[/Begin assertion loop/]
    Assert --> AssertType{Assertion type?}

    AssertType -->|contains, regex,<br/>icontains, is-json,<br/>latency| Det[Evaluate locally in Node<br/><b>Cost: 0 LLM calls</b>]

    AssertType -->|llm-rubric,<br/>factuality,<br/>answer-relevance| LLM[POST /v1/chat/completions<br/>to judge model<br/><b>Cost: 1 LLM call</b>]

    Det --> RecordA[Record assertion verdict<br/>pass / fail + reason]
    LLM --> RecordA

    RecordA --> MoreA{More assertions<br/>in this row?}
    MoreA -->|Yes| Assert
    MoreA -->|No| RowAgg{All assertions<br/>passed?}

    RowAgg -->|Yes| RowPass([Row PASS])
    RowAgg -->|No| RowFail([Row FAIL])

    RowPass --> NextRow{More test rows?}
    RowFail --> NextRow
    RowError --> NextRow
    NextRow -->|Yes| Rows
    NextRow -->|No| SuiteAgg[Aggregate suite totals:<br/>passed · failed · errors]

    SuiteAgg --> Report[Write report/last.html<br/>+ report/last.json]
    Report --> Exit{Any row failed?}
    Exit -->|Yes| ExitNonZero([Exit code 1<br/>CI gate fails])
    Exit -->|No| ExitZero([Exit code 0<br/>CI gate passes])

    classDef startNode fill:#dbeafe,stroke:#1e40af,color:#0c1f4a
    classDef decision fill:#fef3c7,stroke:#92400e,color:#3b1d04
    classDef pass fill:#d1fae5,stroke:#047857,color:#064e3b
    classDef fail fill:#fee2e2,stroke:#b91c1c,color:#7f1d1d
    classDef detPath fill:#e0e7ff,stroke:#3730a3,color:#1e1b4b
    classDef llmPath fill:#fce7f3,stroke:#9d174d,color:#500724

    class Start,Report,SuiteAgg,Load,RenderURL,HitSUT,Extract,RecordA startNode
    class PreCheck,SUTOK,AssertType,MoreA,RowAgg,NextRow,Exit decision
    class RowPass,ExitZero pass
    class RowFail,ExitNonZero,Abort,RowError fail
    class Det detPath
    class LLM llmPath
```

### Reading the control flow

The diagram has three nested loops and four key decision points. Reading
them in order:

| Decision | What it asks | Branches |
|---|---|---|
| **SUT alive?** | Is the Spring app responding? | No → abort cleanly; Yes → continue |
| **HTTP 200?** | Did this specific row's call succeed? | No → record as error, continue to next row; Yes → run assertions |
| **Assertion type?** | Is this assertion deterministic or LLM-graded? | Local eval (zero cost) vs judge call (one LLM call) |
| **Row aggregate** | Did **all** assertions pass? | One failure = row fails (AND semantics) |
| **Suite aggregate** | Did **any** row fail? | One failure = exit code 1 (CI gate fails) |

### Why this view matters for the POC argument

Three things this diagram makes visually obvious that the other two do not:

1. **Cost economics.** The pink path (`llm-rubric`) is one LLM call per
   assertion. The blue path (deterministic) is free. A 100-row suite that
   uses two `contains` + one `llm-rubric` per row costs 100 judge calls.
   The same suite with three `llm-rubric` per row costs 300 — three times
   slower and three times more compute. **Suite design directly drives
   eval cost.**

2. **CI gate contract.** The bottom two diamonds (row aggregate, suite
   aggregate) define the exit-code semantics. Any failed assertion fails
   its row; any failed row fails the suite; any failed suite blocks the
   merge. This is the **standard CI test contract** — promptfoo behaves
   the same way pytest, jest, or junit do. A reviewer who has used any
   of those instinctively understands the gate.

3. **Two independent loops.** The eval is `row_loop → assertion_loop`,
   not one flat list. A single row can have many assertions of mixed
   types. This is why you can layer cheap deterministic checks
   (`contains`, `latency`) with more expensive semantic checks
   (`llm-rubric`) on the same row — and why dropping the cheap ones is
   never a good idea: they catch the obvious failures before any judge
   gets involved.

### When to pull which diagram in the presentation

| Slide goal | Use diagram |
|---|---|
| "Here is the system at a glance" | §1 Topology |
| "Here is how a single eval flows in time" | §2 Lifecycle |
| "Here is why our suite design choices matter" | §3 Control flow (this one) |
| "Here is how the CI gate decides pass/fail" | §3 Control flow (this one) |

---

## 4. Why this design

A few non-obvious properties that make this setup credible as a POC:

- **Promptfoo never touches Spring AI internals.** It speaks plain HTTP. Any
  change inside the Spring app — different controller, different prompt
  template, different provider — is transparent to Promptfoo as long as the
  endpoint contract (`GET /ai/generate?message=&chatId=`, returns
  `{"generation": "..."}`) is preserved.

- **`chatId` is the integration hinge.** It travels in the URL,
  `MessageChatMemoryAdvisor` keys conversation history on it, and reusing it
  across rows is what makes multi-turn memory testing possible without any
  client-side session state.

- **One Ollama process, two distinct roles.** The SUT model is queried via
  the native `/api/chat` endpoint (used by Spring AI). The judge is queried
  via the OpenAI-compatible `/v1/chat/completions` endpoint (used by
  Promptfoo's grader). Same daemon, two protocols, fully independent
  configurations.

- **Judge ≥ SUT.** `llama3.1:8b` judging `llama3.2:1b` is the structural
  asymmetry required for an LLM-as-judge eval to recognize SUT mistakes (see
  [`poc.md` §1](../promptfoo/poc.md#1-goal) for the rationale).

---

## 5. Where to go next

| To understand... | Read... |
|---|---|
| The end-to-end POC goal and acceptance criteria | [`promptfoo/poc.md`](../promptfoo/poc.md) |
| Annotated config, test files, and run modes | [`docs/promptfoo-guide.md`](promptfoo-guide.md) |
| Where Promptfoo fits among the other evaluation tools | [`docs/promptfoo-analysis.md`](promptfoo-analysis.md) |
| The regression-detection demo (3-phase orchestration) | [`promptfoo/regression-demo.md`](../promptfoo/regression-demo.md) |
