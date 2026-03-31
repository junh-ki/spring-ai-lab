# spring-ai-lab

## Startup Guide

This project supports provider switching with Maven profiles and `APP_CHAT_MODEL`.

### Prerequisites

- Java 21
- Maven (or `./mvnw`)
- Running Ollama locally for dev/test (`http://localhost:11434`)

### Provider values

- `ollama`
- `openai`
- `bedrock-converse`

### Development (default: Ollama)

```bash
mvn -Pdev-ollama spring-boot:run -Dspring-boot.run.profiles=dev
```

or explicitly:

```bash
APP_CHAT_MODEL=ollama mvn -Pdev-ollama spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production with OpenAI

```bash
export OPENAI_API_KEY=<your-openai-key>
APP_CHAT_MODEL=openai mvn -Pprod-openai spring-boot:run -Dspring-boot.run.profiles=prod
```

### Production with Bedrock

```bash
export AWS_REGION=eu-central-1
export AWS_ACCESS_KEY=<your-aws-access-key>
export AWS_SECRET_KEY=<your-aws-secret-key>
APP_CHAT_MODEL=bedrock-converse mvn -Pprod-bedrock spring-boot:run -Dspring-boot.run.profiles=prod
```

### Build runnable jar

```bash
mvn -Pdev-ollama clean package
mvn -Pprod-openai clean package
mvn -Pprod-bedrock clean package
```

Run:

```bash
APP_CHAT_MODEL=<ollama|openai|bedrock-converse> java -jar target/spring-ai-lab-0.0.1-SNAPSHOT.jar
```
