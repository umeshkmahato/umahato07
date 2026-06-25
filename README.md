# Umahato Personalization Assistant Microservices

Spring Boot multi-module microservice platform implementing:
- API Gateway with JWT validation/authZ
- Eureka service discovery
- `user-service` with Postgres + EHCache + Redis
- `agent-service` with Redis session memory + Kafka events + external API call via Resilience4j + mocked agentic GenAI orchestrator

## Modules

- `service-discovery`
- `api-gateway`
- `user-service`
- `agent-service`
- `common-lib`

## High-level architecture

```text
Client -> API Gateway (JWT authN/authZ) -> Eureka -> user-service / agent-service

user-service -> Postgres + EHCache (L1) + Redis (L2)
agent-service -> user-service + Redis + Kafka + external score API (circuit breaker) + AgentOrchestrator
```

## Prerequisites

- Java 21
- Maven 3.9+
- Docker + Docker Compose

## Run everything with Docker Compose

1. Build and start:
```bash
docker compose up --build
```

2. Wait for all services to become healthy/started:
- Eureka: `http://localhost:8761`
- Gateway health: `http://localhost:8080/internal/gateway/health`

## Dev Container workflow

1. Open this repository in VS Code.
2. Run **Reopen in Container**.
3. Inside the container:
```bash
./mvnw clean package -DskipTests
docker compose up --build
```

## Issue a test JWT (HS256)

Gateway defaults:
- `JWT_ISSUER=umahato-auth`
- `JWT_AUDIENCE=umahato-api`
- `JWT_SECRET=umahato-dev-secret-key-please-change-12345`

Generate a token:
```bash
python - <<'PY'
import base64, json, hmac, hashlib, time
def b64(x): return base64.urlsafe_b64encode(json.dumps(x,separators=(',',':')).encode()).rstrip(b'=')
header={"alg":"HS256","typ":"JWT"}
payload={
  "iss":"umahato-auth",
  "aud":"umahato-api",
  "sub":"demo-user",
  "scope":"user.read user.write agent.query",
  "iat":int(time.time()),
  "exp":int(time.time())+3600
}
secret=b"umahato-dev-secret-key-please-change-12345"
msg=b'.'.join([b64(header),b64(payload)])
sig=base64.urlsafe_b64encode(hmac.new(secret,msg,hashlib.sha256).digest()).rstrip(b'=')
print((msg+b'.'+sig).decode())
PY
```

Export token:
```bash
export TOKEN="<paste-token>"
```

## End-to-end API flow via gateway

1. Create user:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":101,"email":"alice@example.com","name":"Alice","preferences":"{\"theme\":\"dark\"}"}'
```

2. Read user (demonstrates cacheable path after first call):
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/101
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/101
```

3. Query agent:
```bash
curl -X POST http://localhost:8080/api/agent/query \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"sess-101","userId":101,"query":"summarize profile with score"}'
```

Expected behavior:
- Gateway validates JWT and routes request.
- Agent service fetches user from user-service.
- Agent orchestration generates a tool plan via mocked `LanguageModelClient`.
- Agent stores session context in Redis (`agent-session:sess-101`).
- External score call runs under circuit breaker (fallback returns default if downstream fails).
- Kafka event is published to `user-agent-events`.

## Run tests

```bash
./mvnw test
```

Included tests:
- `user-service`: `UserControllerCachingIntegrationTest` (`/users/{id}` + caching behavior)
- `agent-service`: `AgentQueryOrchestrationTest` (`/agent/query` orchestration with mocks)

## Key ports

- Eureka: `8761`
- Gateway: `8080`
- user-service: `8081`
- agent-service: `8082`
- Postgres: `5432`
- Redis: `6379`
- Kafka: `9092`
- Profile score mock: `8090`

## Additional docs

- `PHASE_PROGRESS.md`
- `PHASE1_ARCHITECTURE_KNOWLEDGE_SHARE.md`