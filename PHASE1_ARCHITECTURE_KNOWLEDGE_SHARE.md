# Architecture Knowledge Sharing (Phase 1 + Phase 2 Update)

Last updated: 2026-06-26 02:53:42 +05:30

## Domain
**Personalization Assistant** with three runtime services behind a gateway:
- `api-gateway` (public entrypoint)
- `user-service` (user profile core domain)
- `agent-service` (agent orchestration and downstream integration)

## High-Level Architecture
```text
Client
  -> API Gateway (JWT authN/authZ)
      -> Eureka Service Discovery
          -> user-service
          -> agent-service

user-service -> Postgres (source of truth)
             -> EHCache (L1 local cache)
             -> Redis (L2 shared cache)

agent-service -> user-service (HTTP)
              -> Redis (short-term session/context)
              -> Kafka (agent events)
              -> External profile-score API (Circuit Breaker)
```

## Why Eureka Service Discovery
- Dynamic service registration and lookup for local microservice workflows.
- Gateway can route using logical service IDs (`lb://user-service`, `lb://agent-service`) instead of hardcoded host/port.
- Keeps the design production-aligned while still simple for local development.

## API Gateway Responsibilities
- Validate JWT from `Authorization: Bearer <token>`.
- Enforce claim checks (`iss`, `aud`, `exp`) and scope/role-based authorization.
- Route `/api/users/**` to `user-service` and `/api/agent/**` to `agent-service`.
- Return consistent `401/403` responses for auth failures.

## Service Contracts (Phase 1 design)

### user-service
- `GET /users/{id}` – fetch profile
- `PUT /users/{id}` – update profile
- `POST /users` – create profile

### agent-service
- `POST /agent/query` – process a user query, call required tools/services, and return structured response

## Data Contracts

### PostgreSQL schema (`users`)
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  preferences TEXT NOT NULL
);
```

### Redis key strategy
- `user:{id}` – shared cache for user profile payloads (TTL)
- `agent-session:{sessionId}` – short-term agent memory/context (TTL)
- `agent-last-result:{sessionId}` – recent response snapshot (TTL)

### Kafka topic and event shape
- Topic: `user-agent-events`
- Example event:
```json
{
  "eventId": "uuid",
  "eventType": "UserAgentQueried",
  "timestamp": "2026-06-26T00:00:00Z",
  "sessionId": "sess-123",
  "userId": 101,
  "query": "summarize my profile",
  "resultStatus": "SUCCESS"
}
```

## Repository Structure Chosen
**Single repository with multiple Maven modules**:
- `service-discovery`
- `api-gateway`
- `user-service`
- `agent-service`
- `common-lib`

### Why this structure
- Shared DTOs/contracts stay centralized in `common-lib`.
- Independent service boundaries are maintained with separate modules.
- Local build/run remains straightforward with one root Maven project.

## Phase 1 Deliverable Summary
- Architecture defined for gateway, discovery, domain service, agent service, storage, cache, messaging, and resilience.
- Base contracts for API, DB, Redis keys, and Kafka events documented.
- Multi-module scaffold created with app entry points and placeholder configs for JWT, Redis, EHCache, Kafka, Circuit Breaker, and discovery.

## Phase 2 Implementation Update

### Implemented in API Gateway
- JWT validation for issuer, audience, and expiry.
- Scope-based authorization:
  - `/api/users/**` requires `user.read` or `user.write`
  - `/api/agent/**` requires `agent.query`
- Service routing configured via Eureka + load-balanced routes:
  - `/api/users/**` -> `lb://user-service`
  - `/api/agent/**` -> `lb://agent-service`

### Implemented in user-service
- PostgreSQL persistence with JPA entity/repository (`users` table).
- REST endpoints implemented:
  - `POST /users`
  - `GET /users/{id}`
  - `PUT /users/{id}`
- Two-level caching flow implemented:
  1. EHCache local lookup (`usersLocal`)
  2. Redis lookup (`user:{id}`)
  3. Postgres fallback
  4. Cache backfill on DB hit
- Write/update flow: DB save + cache synchronization to EHCache and Redis.

### Implemented in agent-service
- `POST /agent/query` implemented.
- Calls `user-service` over HTTP using service discovery.
- Uses Redis session memory key `agent-session:{sessionId}` with TTL.
- Calls external profile score API with Resilience4j circuit breaker (`profileScore`) and fallback score.
- Publishes `UserAgentQueried` event to Kafka topic `user-agent-events`.

### Event shape in use
```json
{
  "eventId": "uuid",
  "eventType": "UserAgentQueried",
  "timestamp": "2026-06-26T00:00:00Z",
  "sessionId": "sess-123",
  "userId": 101,
  "query": "summarize my profile",
  "resultStatus": "SUCCESS",
  "source": "external"
}
```

### Current note
- Phase 2 code is implemented.
- In this environment, full Maven verification is intermittently blocked by external artifact mirror timeout.
