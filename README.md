# Sensitive Words API

Spring Boot 3.5 service for managing a normalized dictionary and masking configured terms. Production persistence is Microsoft SQL Server. Flyway creates the schema and seeds 228 terms once.

## Requirements and local use

Use Java 17. Docker with at least 2 GB available to SQL Server is required for the production-like setup and integration tests. Apple Silicon hosts may require `linux/amd64` emulation.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses disposable H2 storage and HTTP Basic authentication. `admin`/`admin-demo-only` can manage and filter; `client`/`client-demo-only` can only filter. Override passwords with `LOCAL_ADMIN_PASSWORD` and `LOCAL_CLIENT_PASSWORD`.

Authenticated local users can open Swagger UI at `http://localhost:8080/swagger-ui.html` and generated JSON at `/v3/api-docs`. Both are disabled outside the local profile.

## Microsoft SQL Server

```bash
cp .env.example .env
# Set DB_PASSWORD, JWT_ISSUER, and JWT_AUDIENCE.
docker compose up --build -d
curl -f http://localhost:8080/actuator/health/readiness
```

Compose uses SQL Server 2022 Developer edition and accepts its license through `ACCEPT_EULA=Y`. Review Microsoft licensing before use. Connection settings come from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; credentials are not committed.

## API examples

These examples use the `local` profile and its demo credentials. The Compose setup uses JWT authentication; supply a token from your configured issuer for those requests.

```bash
curl -u client:client-demo-only -H 'Content-Type: application/json' \
  -d '{"message":"SELECT * FROM customers"}' http://localhost:8080/api/v1/messages/filter
curl -u admin:admin-demo-only -H 'Content-Type: application/json' \
  -d '{"value":"private phrase"}' http://localhost:8080/api/internal/v1/sensitive-words
printf '["CREATE","DROP","SELECT * FROM"]' >/tmp/words.txt
curl -u admin:admin-demo-only -F 'file=@/tmp/words.txt;type=application/json' \
  http://localhost:8080/api/internal/v1/sensitive-words/import
```

CRUD and import require `words.manage` in production. Filtering requires `messages.filter`. Production uses JWT Bearer authentication. Imports accept a UTF-8 JSON array of strings, are atomic, and are limited to 1 MiB and 10,000 entries.

## Postman collection

1. Import `docs/SensitiveWords.postman_collection.json` into Postman.
2. Set `baseUrl` to the running API, for example `http://localhost:8080` for the local profile. Set the password variables if demo passwords were overridden.
3. In **Import JSON dictionary file**, select `docs/sql_sensitive_list.txt` for the multipart `file` field; local file paths are not embedded in the collection.
4. Run all eight requests in order. The create request captures `wordId`, which subsequent requests use. Assertions check status codes, normalized values, filtering output, deletion and import counts. The collection expects the demo terms `restricted phrase` and `private phrase` to be available for creation/update.

For a JWT-authenticated instance, set the collection authorization to a management token and replace the explicit Basic authentication on the filtering and restricted-client requests with suitable client tokens. Do not change production authentication to run the collection.

## Behavior and architecture

Controllers validate HTTP input. Services own transactions and dictionary rules. `TermNormalizer` trims, collapses Unicode whitespace, and applies locale-independent uppercase normalization. JPA persists normalized terms with a unique constraint. `MessageMasker` performs literal, case-insensitive whole-word matching independently of persistence.

Unicode letters, marks, digits, and underscore are word characters. Phrase whitespace matches one or more Unicode whitespace characters. Longer terms win at the same position. Unmatched text is preserved. `SELECT * FROM` becomes `****** * ****`. Messages are not stored or intentionally logged. Database failures fail filtering rather than returning unfiltered content.

## Tests

```bash
./mvnw verify
./mvnw -Pmssql-it verify
```

The first command runs unit and H2-backed HTTP tests. The second starts actual SQL Server with Testcontainers and requires Docker. Reports are under `target/surefire-reports`, `target/failsafe-reports`, and `target/site/jacoco`.

See [deployment](docs/DEPLOYMENT.md), [validation](docs/VALIDATION.md), [performance](docs/PERFORMANCE.md), and the [Postman collection](docs/SensitiveWords.postman_collection.json).

## Further improvements

- Add an audit trail for dictionary changes, recording actor, operation, entry identifier and time without storing submitted chat messages.
- Add request correlation, latency/error metrics and alerts for database availability and connection-pool saturation.
- If the service needs separate dictionaries for multiple organizations, bind tenant identity to management and filtering queries and add tenant-isolation tests.
- Evaluate matcher caching and insert batching using the workload in the performance notes before adding operational complexity.
