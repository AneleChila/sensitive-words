# Sensitive Words API

Java 17 / Spring Boot 3.5 REST service for an interview assignment. Maintains a database-backed dictionary and masks sensitive terms in submitted chat messages. Includes all 228 supplied entries, including `SELECT * FROM`.

## Run in two commands

Install Java 17+. The included Maven Wrapper downloads Maven on first use (internet access required), or import `pom.xml` in IntelliJ and use its bundled Maven. On Windows use `mvnw.cmd` in place of `./mvnw`.

```bash
./mvnw verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The download also includes a prebuilt JAR in `dist/`. To run without Maven: `java -jar dist/sensitivewords-1.0.0.jar --spring.profiles.active=local`.

The explicit `local` profile uses a disposable H2 database and these demo users:

| User | Password | Access |
|---|---|---|
| admin | admin-demo-only | Dictionary CRUD and filtering |
| client | client-demo-only | Filtering only |

Override with `LOCAL_ADMIN_PASSWORD` and `LOCAL_CLIENT_PASSWORD` if needed. The database resets on application restart in this profile; Flyway reloads the supplied list. Production uses PostgreSQL with durable storage and JWT authentication. **Never activate `local` outside your development machine.**

```bash
curl -u client:client-demo-only http://localhost:8080/api/v1/messages/filter \
  -H 'Content-Type: application/json' \
  -d '{"message":"SELECT * FROM customers; please DROP TABLE customers"}'
```

Response:

```json
{"message":"****** * **** customers; please **** ***** customers"}
```

## Endpoints

| Method | Path | Successful result | Production scope |
|---|---|---|---|
| POST | `/api/internal/v1/sensitive-words` | 201 + body and Location | `words.manage` |
| GET | `/api/internal/v1/sensitive-words?page=0&size=20` | 200 + paginated list | `words.manage` |
| GET | `/api/internal/v1/sensitive-words/{id}` | 200 + word | `words.manage` |
| PUT | `/api/internal/v1/sensitive-words/{id}` | 200 + updated word | `words.manage` |
| DELETE | `/api/internal/v1/sensitive-words/{id}` | 204, empty body | `words.manage` |
| POST | `/api/v1/messages/filter` | 200 + amended message | `messages.filter` |
| GET | `/actuator/health/liveness` | 200 while process is healthy | Public probe |
| GET | `/actuator/health/readiness` | 200 when ready, 503 otherwise | Public probe |

Word write payload: `{"value":"some sensitive phrase"}`. Word response: `{"id":"<uuid>","value":"SOME SENSITIVE PHRASE"}`. Create generates a UUID. PUT replaces an existing entry; it does not upsert. Missing IDs return 404; normalized duplicates return 409. Database uniqueness also covers concurrent create/update races. Simultaneous updates use JPA optimistic locking and return 409 if they conflict; sequential updates use last-write-wins.

Pagination is zero-based, sorted by normalized value, with size 1–100. Invalid input returns 400; unauthenticated access 401; insufficient permissions 403. MVC errors use Problem Details; security-layer 401/403 responses may have no JSON body. Messages are required strings, at most 10,000 Java UTF-16 code units; empty strings are allowed. Words must be nonblank and at most 255 code units before and after normalization.

```bash
# Add a word. Copy the returned id for the remaining requests.
curl -i -u admin:admin-demo-only http://localhost:8080/api/internal/v1/sensitive-words \
  -H 'Content-Type: application/json' -d '{"value":"secret"}'
curl -u admin:admin-demo-only 'http://localhost:8080/api/internal/v1/sensitive-words?size=5'
curl -u admin:admin-demo-only http://localhost:8080/api/internal/v1/sensitive-words/REPLACE_WITH_ID
curl -X PUT -u admin:admin-demo-only http://localhost:8080/api/internal/v1/sensitive-words/REPLACE_WITH_ID \
  -H 'Content-Type: application/json' -d '{"value":"private phrase"}'
curl -X DELETE -u admin:admin-demo-only http://localhost:8080/api/internal/v1/sensitive-words/REPLACE_WITH_ID
```

An importable Postman collection is in `docs/SensitiveWords.postman_collection.json`; the OpenAPI contract is in `docs/openapi.yaml`.

## Matching decisions and ambiguities

The brief leaves matching semantics open. These are explicit implementation assumptions:

* Match case-insensitively using Java Unicode case matching. Dictionary writes trim surrounding whitespace, collapse whitespace to one space, and uppercase using `Locale.ROOT`; the message itself is never normalized.
* Match complete terms, not substrings inside identifiers or words. Unicode letters, combining marks, digits and underscore count as word characters. `DROP` matches `drop!` but not `dropdown`, `MY_DROP` or `DROP2`.
* Prefer the longest term at a shared starting position. Matching scans left to right, taking non-overlapping matches. Regex metacharacters in entries are quoted and treated literally.
* Phrase spaces match one or more Unicode whitespace characters, including newlines. Preserve the original whitespace, punctuation outside a match, and all unmatched text. Replace each non-whitespace Unicode code point within a match with one `*`.
* `SELECT * FROM` becomes `****** * ****`. The PDF's shorter sample leaves `FROM` visible, but its supplied list explicitly includes the full phrase; this solution masks the phrase. Confirm this assumption with the interviewer if they expect the sample literally.
* No stemming, fuzzy matching, accent folding, Unicode compatibility normalization, or evasion detection. A Unicode case match is not a full linguistic equivalence algorithm. This is configurable content masking, not SQL injection prevention; database access still uses bound parameters through JPA.

## Design

Controllers validate requests and map DTOs; transactional services enforce dictionary rules; Spring Data JPA persists entries. `MessageMasker` is a pure component and has no database dependency. Flyway creates the schema and seeds the supplied list once per database, so deleted seed entries are not silently resurrected on restart. The original JSON-format list is retained in `docs/sql_sensitive_list.txt`.

Each filtering request reads one committed dictionary snapshot via one database SELECT, then builds a literal longest-first regex. This keeps all replicas consistent with committed CRUD updates without cache invalidation. A request concurrent with an update may use the previous committed snapshot. A DB failure fails the filter request; the service does not silently return unfiltered text. Chat messages are neither stored nor explicitly logged, including rejected validation values.

For 228 entries this intentionally favors clarity. Regex construction and database reads happen per request and are not performance-tested. For a large dictionary or sustained high throughput, benchmark compiled immutable snapshots or an Aho–Corasick matcher; use a dictionary version and cross-replica invalidation with a defined freshness bound. Limit dictionary size/term length and filter request rate at the platform boundary. Authentication identifies callers; this version has one shared company dictionary, not tenant isolation.

## Verification

`mvn verify` runs pure matcher tests and full HTTP/DB tests with H2 in PostgreSQL compatibility mode, including Flyway. Tests cover CRUD-to-filter propagation, duplicates, validation, phrase priority, literal regex characters, Unicode boundaries, pagination and authorization. Production JWT routing has separate tests with a stub decoder; the identity provider remains an environment integration to validate.

H2 does not prove PostgreSQL compatibility. Before a real rollout run the PostgreSQL smoke procedure in `docs/DEPLOYMENT.md`, exercise your actual identity provider, and load-test representative data. Docker and PostgreSQL are not prerequisites for the local demo.

## Production

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for the deployment walkthrough, configuration, security boundaries, CI/CD, migration, scaling, monitoring and rollback. Nothing is deployed by this project.

Reference documentation: [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Spring Security JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
