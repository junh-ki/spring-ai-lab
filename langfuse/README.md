# Langfuse — observability for the LLM stack

Langfuse captures every model call (prompts, completions, tools, retrievals, latencies, token costs) and exposes a UI plus an API on top. In this repo it traces the Spring AI app via OTLP — wiring is in [TracingConfig.java](../src/main/java/com/example/springailab/config/TracingConfig.java) and [application.yaml](../src/main/resources/application.yaml).

## Local use

This stack runs as its own compose project (`spring-ai-lab-langfuse`, defined in [docker-compose.yml](docker-compose.yml)) so trace data, project keys, and migrations persist across `./start-demo.sh` invocations.

```bash
./start-demo.sh                      # brings Langfuse up if not already running
open http://localhost:3000           # demo@local.dev / demo1234
./langfuse/stop.sh                   # stop, keep volumes
./langfuse/stop.sh --wipe            # stop and wipe trace data + keys
LANGFUSE_ENABLED=0 ./start-demo.sh   # skip Langfuse entirely
```

OTLP credentials baked in via `LANGFUSE_INIT_*`: `pk-lf-spring-ai-lab` / `sk-lf-spring-ai-lab`. Matched by the `Authorization` header in [application.yaml](../src/main/resources/application.yaml).

## What Langfuse does

- **Tracing** — hierarchical spans for chat, tools, retrievals; sessions group multi-turn conversations
- **Cost tracking** — per-token cost across providers (OpenAI, Anthropic, Bedrock, Ollama, custom)
- **Prompt management** — prompt registry with versioning and environment labels, pulled at runtime
- **Evaluations** — datasets, LLM-as-judge, programmatic evaluators (Langfuse-native equivalent of [deepeval/](../deepeval/))
- **Multi-tenancy** — orgs and projects with per-project API keys; SSO (OIDC in OSS, SAML in enterprise)
- **Integrations** — native SDKs (Python/JS), OTLP for any language (Java/Spring AI uses this), LangChain/LlamaIndex wrappers

## Running this in production

Use the official Helm chart (`langfuse/langfuse-k8s`). Key placement decisions:

| Component | Production placement |
|---|---|
| `langfuse-web`, `langfuse-worker` | K8s Deployments, autoscaled (worker on Redis queue depth) |
| Postgres | Managed (RDS / Cloud SQL); holds project keys + prompts — back this up |
| Clickhouse | Clickhouse Cloud, or self-host with persistent volumes; sized to retention × event-rate |
| Redis | Managed (ElastiCache / Memorystore) |
| Object storage | AWS S3 / GCS via IRSA — don't bake credentials |

Rough sizing for a multi-team org at 100K–5M events/day: managed Postgres at 4 vCPU / 16 GB / 200 GB SSD, Clickhouse at 4–8 vCPU / 16–32 GB / 500 GB SSD, 2 web replicas + 2–4 worker replicas. **Set Clickhouse TTL on traces from day one** — storage dominates cost. Bake `ENCRYPTION_KEY`, `SALT`, `NEXTAUTH_SECRET` into your secret manager (rotating `ENCRYPTION_KEY` re-encrypts API keys, plan once).

For one centralized AI platform serving multiple teams: one Langfuse instance, one project per service, services authenticate with their own `pk`/`sk`. W3C trace context propagation means cross-service spans show up under the same trace.

## Self-host vs Cloud

- **Cloud** (langfuse.com) — cheaper at low/medium volume, fine when data residency permits.
- **Self-host** — required when traces must stay in your VPC, or when event volume makes per-event cloud pricing exceed self-host TCO.

Pragmatic path: start on Cloud while proving value, migrate to Helm once volume justifies the ops cost.

## Deeper docs

- `langfuse.com/docs` — self-hosting, retention, scaling
- `github.com/langfuse/langfuse`, `github.com/langfuse/langfuse-k8s`
