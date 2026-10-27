# Deployment

Run the stateless API behind TLS with SQL Server on a private network. Expose only `POST /api/v1/messages/filter` publicly. Keep `/api/internal/**`, Swagger, and actuator routes on private administration networks. URL naming is not a security boundary.

| Variable | Purpose |
|---|---|
| `DB_URL` | SQL Server JDBC URL, for example `jdbc:sqlserver://db:1433;databaseName=sensitivewords;encrypt=true;trustServerCertificate=false` |
| `DB_USERNAME` | Application database identity |
| `DB_PASSWORD` | Secret-managed password |
| `JWT_ISSUER` | Trusted OIDC issuer |
| `JWT_AUDIENCE` | Required audience; defaults to `sensitivewords-api` |
| `PORT` | HTTP port; defaults to 8080 |

Use a trusted server certificate and `trustServerCertificate=false` in production. Never enable the `local` profile there. Swagger and generated OpenAPI are disabled by default.

Flyway V1 creates SQL Server-compatible Unicode storage and normalized uniqueness. V2 inserts 228 initial terms once. Deleted terms are not restored on restart. Changing a PostgreSQL JDBC URL does not migrate existing data. Export existing normalized terms, validate and import them into SQL Server during a maintenance window, reconcile counts, and retain the source backup until verification completes.

On a fresh database verify 228 entries, modify a term, restart the API, and confirm persistence. Verify client tokens cannot access dictionary routes and filtering fails when SQL Server is unavailable. `docker compose down` preserves the named volume; `docker compose down -v` deletes it.

Use `/actuator/health/liveness` for process health and `/actuator/health/readiness` for traffic gating. Monitor latency, errors, JVM memory, pool saturation, readiness, and dictionary size without recording message bodies or Authorization headers. Back up SQL Server and rehearse restore. Roll back the application image only while migrations remain backward compatible.
