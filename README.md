# Umahato Personalization Assistant Microservices

Spring Boot multi-module microservice system scaffold for a personalization assistant domain.

## Modules

- `service-discovery`: Eureka server for service registration and lookup
- `api-gateway`: public entrypoint (planned JWT validation + routing)
- `user-service`: user profile domain service (planned Postgres + EHCache + Redis)
- `agent-service`: agent orchestration service (planned Redis + Kafka + downstream HTTP + circuit breaker)
- `common-lib`: shared DTOs/contracts

## Architecture (Phase 1)

```text
Client
  -> API Gateway (JWT authN/authZ)
      -> Eureka Service Discovery
          -> user-service
          -> agent-service

user-service -> Postgres + EHCache + Redis
agent-service -> user-service + Redis + Kafka + external API (circuit breaker)
```

## Current Status

- ✅ **Phase 1 complete**: architecture + multi-module scaffolding
- ⏳ **Phase 2 pending**: JWT, DB, caching, circuit breaker, Kafka flow
- ⏳ **Phase 3 pending**: agentic orchestrator, Docker, Dev Container, E2E tests

Detailed docs:
- `PHASE_PROGRESS.md`
- `PHASE1_ARCHITECTURE_KNOWLEDGE_SHARE.md`

## Prerequisites

- Java 21
- Maven 3.9+ (or use `./mvnw`)

## Build

```bash
./mvnw clean install
```

## Run Locally

Start each service in a separate terminal, in this order:

1. **Service Discovery**
```bash
./mvnw -pl service-discovery spring-boot:run
```

2. **User Service**
```bash
./mvnw -pl user-service spring-boot:run
```

3. **Agent Service**
```bash
./mvnw -pl agent-service spring-boot:run
```

4. **API Gateway**
```bash
./mvnw -pl api-gateway spring-boot:run
```

## Default Ports

- Eureka: `8761`
- API Gateway: `8080`
- User Service: `8081`
- Agent Service: `8082`

## Quick Checks

```bash
curl http://localhost:8761
curl http://localhost:8080/internal/gateway/health
```

## Planned APIs (next phases)

- `GET /users/{id}`
- `PUT /users/{id}`
- `POST /users`
- `POST /agent/query`

## Tech Stack

- Spring Boot
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Data JPA (Postgres)
- Redis
- EHCache
- Kafka
- Resilience4j