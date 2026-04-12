# spring-ai-lab

Playground for Spring AI: chat, RAG, and related experiments.

## Quick start (local demo)

**Prerequisites:** Java 21, Docker with Compose (`docker compose`).

```bash
./start-demo.sh
```

This script:

1. Runs **`docker compose up -d`** using [`docker-compose.yml`](docker-compose.yml) (Redis on `6379`, Ollama on `11434`, models stored in a Docker volume).
2. Waits until Redis and Ollama respond (no host `redis-cli` required).
3. Pulls the chat and embedding models (defaults: `llama3.2:1b`, `nomic-embed-text` — small enough for typical Docker/macOS RAM limits).
4. Starts the app with `./mvnw -Pdev-ollama spring-boot:run` and profile `dev` (in-memory `SimpleVectorStore`, Ollama chat + embeddings).

**Stopping:** Press **Ctrl+C** (or send **SIGTERM/SIGHUP**). The script then stops the Maven/Spring Boot process, runs **`docker compose down -v`** for this project (removes containers **and** named volumes — including the Ollama model cache), and kills any remaining JVM running `SpringAiLabApplication`. If you started Compose yourself without this script, tear down the same way from the repo root: `docker compose down -v`.

**Larger chat models:** If Ollama errors with “model requires more system memory than is available”, stay on the default `llama3.2:1b` or set e.g. `OLLAMA_MODEL=llama3.2:3b` / `llama3.1` only after giving Docker (or your host) more RAM. Example:

```bash
OLLAMA_MODEL=llama3.1 ./start-demo.sh
```

**Port conflicts:** If `6379` or `11434` is already in use, stop the other service or override ports, for example:

```bash
REDIS_HOST_PORT=6380 OLLAMA_HOST_PORT=11435 OLLAMA_BASE_URL=http://127.0.0.1:11435 ./start-demo.sh
```

**Manual run** (Compose stack already up, models already pulled):

```bash
./mvnw -Pdev-ollama spring-boot:run -Dspring-boot.run.profiles=dev
```

## Try the HTTP API (LLM text in the response)

**Why `curl` printed nothing:** the README used `PASS` as a placeholder. If the password is wrong, Spring Security returns **401** and often an **empty body**, while `curl -s` also hides the error line—so you see a blank line.

**With `./start-demo.sh`** (or any run with Spring profile **`dev`**), Basic auth is fixed in [`application-dev.yaml`](src/main/resources/application-dev.yaml): user **`user`**, password **`demo`**. Use **`demo`**, not the word `PASS`.

**If you still get nothing:** run `curl -i -u user:demo "http://localhost:8080/ai/generate?message=hi"` and check for `HTTP/1.1 401` vs `200`. Omit `-s` until you see `200 OK`.

Below are the smallest set of endpoints whose **body is the model’s answer** (plain text or a JSON object with a `generation` field). Other controllers return structured DTOs, search hits, or **non-LLM** logic (for example `POST /api/context/trim` only shortens messages locally—no model call).

| Endpoint | What you see |
|----------|----------------|
| `GET /ai/generate` | JSON: `{"generation":"..."}` — general chat + memory |
| `GET /poem` | Plain text poem |
| `GET /support` | Plain text RAG answer (ingest PDFs first if you want grounded answers) |
| `GET /agent/chat` | Plain text reply (may call tools, e.g. flights) |

Examples (profile **`dev`**, password **`demo`**):

```bash
curl -u user:demo "http://localhost:8080/ai/generate?message=Explain+Spring+AI+in+two+sentences&chatId=demo"
curl -u user:demo "http://localhost:8080/poem?topic=winter+in+the+city"
curl -u user:demo "http://localhost:8080/support?question=What+is+this+app+for%3F"
curl -u user:demo "http://localhost:8080/agent/chat?message=Say+hello+in+one+short+sentence"
```

Optional: **streaming** tokens (SSE; same auth):

```bash
curl -N -u user:demo "http://localhost:8080/ai/generateStream?message=Count+from+1+to+3"
```

## Provider profiles

Chat provider is selected with `spring.ai.model.chat` / `APP_CHAT_MODEL` and the matching Maven profile:

| Provider         | Maven profile  | Typical `APP_CHAT_MODEL` |
|-----------------|----------------|---------------------------|
| Ollama (default) | `dev-ollama`   | `ollama`                  |
| OpenAI          | `prod-openai`  | `openai`                  |
| Bedrock         | `prod-bedrock` | `bedrock-converse`        |

### OpenAI

```bash
export OPENAI_API_KEY=<your-key>
APP_CHAT_MODEL=openai ./mvnw -Pprod-openai spring-boot:run -Dspring-boot.run.profiles=prod
```

### AWS Bedrock

```bash
export AWS_ACCESS_KEY=<key>
export AWS_SECRET_KEY=<secret>
export AWS_REGION=eu-central-1
APP_CHAT_MODEL=bedrock-converse ./mvnw -Pprod-bedrock spring-boot:run -Dspring-boot.run.profiles=prod
```

## Build

```bash
./mvnw -Pdev-ollama clean package
java -jar target/spring-ai-lab-0.0.1-SNAPSHOT.jar
```

Use `-Pprod-openai` or `-Pprod-bedrock` when building for those providers; set `APP_CHAT_MODEL` and provider credentials when running the jar.
