# Production deployment walkthrough

## 1. Architecture and boundaries

Run the stateless API as a container on your existing orchestrator (for example Kubernetes), backed by managed PostgreSQL in a private network. The public gateway exposes **only** `POST /api/v1/messages/filter`. Route dictionary endpoints through a private ingress reachable by approved internal tooling. The word `internal` in a URL is not a security boundary: ingress allowlists, network controls, and scope checks enforce the distinction.

Use your OAuth2/OIDC identity provider. External caller tokens need `messages.filter`; internal operators need `words.manage`. Validate signature, issuer, expiry/not-before and audience. The production decoder uses issuer discovery and validates the audience configured below. Scope claims map to Spring's `SCOPE_` authorities. The API issues no tokens. Do not embed a confidential client secret in a mobile/browser app; use the identity provider's suitable user flow or an authenticated backend-for-frontend. JWT Bearer auth is stateless; CSRF is disabled for this API. The opt-in local HTTP Basic profile is for development only.

Terminate HTTPS at the gateway, encrypt the database connection, and restrict database ingress to application infrastructure. Browser cross-origin support is not enabled by default; configure a narrow CORS allowlist if your client needs it. Do not expose actuator endpoints publicly through ingress, even though probes have no token requirement.

## 2. Build and configuration

CI runs `mvn -B verify`, dependency/security scanning, and container scanning. Build the supplied Dockerfile; it runs tests in its build stage and runs the final JVM as a non-root user. Pin reviewed container digests, use a private registry, tag by commit SHA, and promote the same image through staging and production. Recheck supported Java/Spring releases and dependency advisories before deployment.

Required production configuration:

| Variable | Example / purpose |
|---|---|
| `DB_URL` | `jdbc:postgresql://db.internal:5432/sensitivewords?sslmode=verify-full` |
| `DB_USERNAME` | Application database username |
| `DB_PASSWORD` | Secret-managed database password |
| `JWT_ISSUER` | Exact trusted issuer URL, reachable for discovery/JWKS |
| `JWT_AUDIENCE` | Intended API audience; default `sensitivewords-api` |
| `PORT` | Optional HTTP listen port; default 8080 |

Mount the trusted PostgreSQL CA and configure JDBC root certificate options for `verify-full`. Keep credentials in a platform secret manager, inject them at runtime, and rotate them. Never set `SPRING_PROFILES_ACTIVE=local` in staging or production. Missing required configuration fails startup rather than enabling demo access. The issuer must be reachable during initial decoder discovery.

## 3. Database migration

Create a dedicated database. Flyway applies V1 (schema) and V2 (all 228 entries). Its schema history prevents replay on restart. Back up existing databases before migrations and validate checksums in staging. Do not edit already-applied migrations; introduce V3, V4, etc.

For this small submission Flyway runs at application startup; its database lock serializes simultaneous migration attempts. For a mature deployment, run migrations as a separate controlled release job using a DDL-capable identity, then grant the runtime identity only the permissions it needs and disable startup Flyway (`SPRING_FLYWAY_ENABLED=false`). Use expand/contract migrations so both old and new replicas work during rolling updates.

## 4. PostgreSQL + JWT smoke test

Prerequisites: Docker Compose and an accessible OIDC issuer with an internal/operator token and a client token for this audience and the respective scopes.

```bash
cp .env.example .env
# Edit .env: set DB_PASSWORD, JWT_ISSUER and JWT_AUDIENCE.
docker compose up --build -d
curl -f http://localhost:8080/actuator/health/readiness

# Obtain short-lived tokens from your issuer; do not commit them.
export ADMIN_TOKEN='replace-with-operator-token'
export CLIENT_TOKEN='replace-with-client-token'
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  'http://localhost:8080/api/internal/v1/sensitive-words?size=1'
curl -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"message":"SELECT * FROM customers"}' \
  http://localhost:8080/api/v1/messages/filter
```

Expect 228 total entries on a fresh database and `****** * **** customers` from filtering. Exercise create, update, delete and duplicate behavior with the operator token. A client token must receive 403 on every dictionary route; no token must receive 401. Verify expired tokens, wrong audience, wrong issuer and invalid signatures are rejected. Restart the API and confirm your dictionary edits persist. Temporarily stop the DB and check readiness becomes 503 and filtering fails instead of passing through chat text. Review logs for unintended message content. `docker compose down` preserves the named database volume; deleting that volume destroys the demo database.

## 5. Rollout, capacity and reliability

Start with two API replicas spread across nodes/zones. Configure CPU/memory requests and limits based on load tests (a starting experiment might use 250m requested CPU and 512 MiB requested memory, not an established capacity claim). The JVM caps heap at 75% of container memory; leave enough headroom for threads, metaspace and native allocations. Use a rolling update with readiness gating and graceful termination.

Point liveness at `/actuator/health/liveness` and readiness at `/actuator/health/readiness`; use a startup probe to allow migrations and issuer discovery. Readiness includes the database; liveness does not, to avoid restarting every replica during a DB outage. A readiness check does not guarantee the very next DB request succeeds. Bound DB connection acquisition and gateway request timeouts; align the gateway deadline with the client deadline. Each replica can use up to 10 DB connections by default; reserve capacity across maximum replica count and migration/operations clients.

At the gateway enforce a request-body limit (for example 64 KiB), authenticated per-client rate limits, connection limits, TLS, and abuse protection. Application validation limits message length after JSON parsing; it does not itself limit raw HTTP request bytes. Configure timeouts and do not buffer unlimited request bodies upstream. Clients should not display raw unfiltered messages on an API error. The filter is stateless and can be retried with bounded backoff; do not retry dictionary POST blindly after an ambiguous timeout.

No broker, Redis or asynchronous worker is necessary for this synchronous task. Add them only when measured requirements justify the operational cost. Use a single shared dictionary now; a multi-company service would need tenant identity bound to both CRUD and filtering queries, compound uniqueness and dedicated isolation tests.

## 6. Monitoring and rollback

Collect request latency, throughput, status codes, JVM memory/GC, DB pool saturation, readiness failures, dictionary size and filter failure rate. Add metrics/tracing integration appropriate to your platform; this starter exposes health/info, not a Prometheus scrape endpoint. Use request/correlation IDs, never raw request bodies or Authorization headers. Audit dictionary edits by actor, action and entry ID in a protected audit sink; a durable audit trail is a production follow-up, not implemented in the starter.

Alert on sustained errors/latency and unavailable replicas. Enable managed PostgreSQL backups and point-in-time recovery, and test restoration. Use a staging smoke test and gradual rollout. Roll back the container image when schemas remain backward-compatible; never automatically undo a destructive database migration. Restore from backup only through a rehearsed incident procedure with explicit acceptance of data loss/time objectives.

## Delivery validation boundaries

See `VALIDATION.md` for the exact checks executed in the build environment. Container builds, PostgreSQL and a real OIDC issuer require their own environment validation; the deployment plan is not evidence those systems have been deployed or tested.
