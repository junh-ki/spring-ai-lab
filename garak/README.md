# Garak POC for `/ai/generate`

This is a simple Garak proof-of-concept, focused only on:

- `GET /ai/generate?message=...&chatId=...`

## What this does

- Uses Garak's `rest.RestGenerator`
- Sends prompts to your Spring endpoint
- Reads the response field from JSON key `generation`
- Runs a small probe set (`dan`) by default for a quick smoke test

## How it works

![Garak POC architecture](img/garak-arch.png)

The diagram shows the entry script, Garak CLI + `generator_options.json`, the REST generator calling `GET /ai/generate`, the Spring app and **Ollama** (model backend — same `:11434` stack as `./start-demo.sh`), plus the probe → detector → report path. Unlike DeepEval, this POC **does not** call Ollama for a separate judge; only Garak’s built‑in detectors score outputs.

## Prerequisites

- Spring app is running locally on `http://localhost:8080`
- `/ai/generate` is reachable with Basic auth `user:demo`
- Python 3.10+ available as `python3`

## Run

```bash
cd garak
chmod +x run.sh
./run.sh
```

## Useful options

```bash
# Run a different probe family
GARAK_PROBES=promptinject ./run.sh

# Increase generations per prompt
GARAK_GENERATIONS=2 ./run.sh

# Pass native garak flags
./run.sh --report_prefix spring-ai-garak
```

## Notes

- The generator config is in `generator_options.json`.
- **`$INPUT` must appear in `req_template`** — Garak does **not** replace `$INPUT` inside `uri`. For GET use `uri`: `http://localhost:8080/ai/generate` and **`req_template`**: `chatId=garak-poc&message=$INPUT` (see [`generator_options.json`](generator_options.json)).
- Default auth header is hardcoded for local demo credentials (`user:demo`).
- Runtime config is written to `garak/.config` via `XDG_CONFIG_HOME` to avoid host permission issues.
- Regenerate the diagram: `pip install pillow && python scripts/render_architecture_png.py` (from `garak/`, writes `img/garak-arch.png`).

### Traceback inside `urllib3` / `http.client` while reading HTTP status (`RemoteDisconnected`)

Usually one of:

1. **Request line length** — probes send very long prompts; Tomcat rejects or drops oversized HTTP request lines. Fix: **`server.max-http-request-header-size`** raised in repo root [`application.yaml`](../src/main/resources/application.yaml) (restart the Spring app).
2. **Broken GET wiring** — if `uri` contained `…&message=$INPUT` literally, URLs were malformed; use the **`req_template` query string** form above instead.
