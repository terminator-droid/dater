# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current sprint

See `SPRINTS.md` for the full sprint plan. At the start of each session: read `SPRINTS.md`, identify the first incomplete sprint, and focus work there. Mark tasks `[x]` as they are completed.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run all tests (spins up Testcontainers — requires Docker)
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName
```

## Architecture

Spring Boot 4.0.6 / Java 17 monolith, designed for eventual microservice extraction. Full architecture details are in `ARCHITECTURE.md`.

**Data stores and their roles:**
- **PostgreSQL** — primary relational data: users, venues, swipes, matches, evening plans. Migrations via Liquibase.
- **MongoDB** — chat messages and conversation topic templates (variable structure, high write throughput).
- **Redis** — real-time geo-index (`GEORADIUS` for nearby users), online TTL status, profile cache, swipe rate limiting, already-swiped sets.
- **Kafka** — three topics: `swipe-events` (every swipe), `match-events` (mutual like detected), `notification-events` (push/email delivery).

**Package layout** (`com.dudev.datingapp`):
Each domain module (`user`, `venue`, `plan`, `discovery`, `swipe`, `match`, `chat`, `topic`, `notification`) follows the pattern `entity/ → repository/ → service/ → dto/ → controller/`. MongoDB documents live in `document/` instead of `entity/`. Kafka producers go in `event/`, consumers in `consumer/`. Cross-cutting concerns: `config/`, `security/`, `common/`.

**Key flows:**
- *Swipe → Match*: POST /swipes writes to PG and publishes a `SwipeEvent`; the Kafka consumer checks for a reverse LIKE and, if found, creates a Match in PG and publishes a `MatchEvent`.
- *Discovery*: when a user creates an EveningPlan, their location is written to Redis GEOADD; GET /discover queries Redis GEORADIUS and filters already-swiped users from PG.
- *Post-match detail*: combines PG (match + partner's EveningPlan: drink_tonight + topic_ids) and MongoDB (TopicService resolves topic_ids to full text). No chat — users meet in person. Partner's name/age are never revealed in cards or match detail.

**Testing:** All integration tests use Testcontainers (`TestcontainersConfiguration`) — PostgreSQL, MongoDB, Kafka, and Redis containers are auto-wired via `@ServiceConnection`. No manual infra setup needed.
