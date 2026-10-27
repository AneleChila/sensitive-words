# Validation record

## Commands

- `./mvnw -q test`: unit, security, H2-backed HTTP, large-import, and generated OpenAPI checks.
- `./mvnw -B -Pmssql-it verify`: the same checks plus Flyway and lifecycle coverage on Testcontainers SQL Server.
- GitHub Actions `test`: Maven verification with Surefire and JaCoCo artifacts.
- GitHub Actions `mssql-integration`: actual SQL Server verification with Failsafe reports.

## Runtime acceptance

The Postman CLI bundled with Postman 1.70.0 ran `docs/SensitiveWords.postman_collection.json` against the local profile over HTTP on 27 October 2026. The run supplied `docs/sql_sensitive_list.txt` to the multipart request and executed 8 requests with 0 failures and 5 assertions. This run used the H2 local profile.

The same collection was then run against the application on port 18083 with SQL Server 2022 in Docker (`mcr.microsoft.com/mssql/server:2022-latest`, exposed locally on 11433). It executed 8 requests with 0 failures and 5 assertions. Direct HTTP checks against this MSSQL run verified readiness 200, 228 migrated entries, CRUD statuses (201/200/200/204/404), normalized duplicate 409, malformed import 400, 10,001-entry limit 400, a 2,501-entry import (`received=2501`, `inserted=2501`, `skippedDuplicates=0`), repeat import (`inserted=0`, `skippedDuplicates=2501`), invalid-import rollback, exact phrase filtering, 401/403 management restrictions, and Swagger UI/assets/OpenAPI responses. The application was stopped and restarted against the same SQL Server volume; readiness remained 200, the dictionary contained 2,729 entries, and the imported `mssql-bulk-2500` term remained masked after restart.

Real OIDC discovery and signatures, container image build, deployment infrastructure, backup restore, failover, and controlled performance measurements require their target environments. The measured performance notes and recommendations remain in `docs/PERFORMANCE.md`.
