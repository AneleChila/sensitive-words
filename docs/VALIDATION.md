# Validation record

## Commands

- `./mvnw -q test`: unit, security, H2-backed HTTP, large-import, and generated OpenAPI checks.
- `./mvnw -B -Pmssql-it verify`: the same checks plus Flyway and lifecycle coverage on Testcontainers SQL Server.
- GitHub Actions `test`: Maven verification with Surefire and JaCoCo artifacts.
- GitHub Actions `mssql-integration`: actual SQL Server verification with Failsafe reports.

## Recorded runtime acceptance (27 October 2026)

Postman CLI 1.70.0 ran `docs/SensitiveWords.postman_collection.json` against the local profile over HTTP on 27 October 2026. The run supplied `docs/sql_sensitive_list.txt` to the multipart request and executed 8 requests with 0 failures and 5 assertions. This run used the H2 local profile.

The same collection was then run against the application on port 18083 with SQL Server 2022 in Docker (`mcr.microsoft.com/mssql/server:2022-latest`, exposed locally on 11433). It executed 8 requests with 0 failures and 5 assertions. Direct HTTP checks against this MSSQL run verified readiness 200, 228 migrated entries, CRUD statuses (201/200/200/204/404), normalized duplicate 409, malformed import 400, 10,001-entry limit 400, a 2,501-entry import (`received=2501`, `inserted=2501`, `skippedDuplicates=0`), repeat import (`inserted=0`, `skippedDuplicates=2501`), invalid-import rollback, exact phrase filtering, 401/403 management restrictions, and Swagger UI/assets/OpenAPI responses. The application was stopped and restarted against the same SQL Server volume; readiness remained 200, the dictionary contained 2,729 entries, and the imported `mssql-bulk-2500` term remained masked after restart.

## Collection regression check (27 October 2026)

The collection now includes assertions for every request, covering status codes, the created identifier and Location header, normalized values, exact masked output, an empty deletion response, and valid import counts. After this change, `mvn -B verify` passed all 25 unit and HTTP tests. The packaged application started with the `local` profile and H2, and readiness returned 200. Newman executed the revised collection against that running application: 8 requests, 17 assertions, 0 failures. The multipart request used the supplied dictionary file. This additional run does not replace the recorded MSSQL acceptance checks above.

## Follow-up validation

Target identity-provider integration, the production image and deployment configuration, backup restoration, and failover exercises remain unverified. Controlled performance and load benchmarks have not been run; these can start in a representative development or staging environment. The proposed workload and performance recommendations are documented in `docs/PERFORMANCE.md`.
