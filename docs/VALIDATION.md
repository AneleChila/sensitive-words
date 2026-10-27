# Validation record

## Commands

- `./mvnw -q test`: unit, security, H2-backed HTTP, large-import, and generated OpenAPI checks.
- `./mvnw -B -Pmssql-it verify`: the same checks plus Flyway and lifecycle coverage on Testcontainers SQL Server.
- GitHub Actions `test`: Maven verification with Surefire and JaCoCo artifacts.
- GitHub Actions `mssql-integration`: actual SQL Server verification with Failsafe reports.

## Runtime acceptance

The Postman CLI bundled with Postman 1.70.0 ran `docs/SensitiveWords.postman_collection.json` against the local profile over HTTP on 27 October 2026. The temporary run supplied `docs/sql_sensitive_list.txt` to the multipart request and executed 8 requests with 0 failures and 5 assertions. The application database for this run was H2 in MSSQL compatibility mode; it is not an MSSQL runtime result.

The MSSQL test covers the 228-row migration, CRUD-to-filter visibility, normalized duplicate conflict, a 2,502-entry import with accurate counts, invalid-import rollback, and deletion visibility. Exact executed results are recorded in PR #9.

The Docker client is installed, but no Docker daemon/socket is available in this workspace, so an additional local MSSQL HTTP run and restart-persistence check could not be executed. GitHub Actions provides the actual SQL Server Testcontainers run. Real OIDC discovery and signatures, container image build, deployment infrastructure, backup restore, failover, and controlled performance measurements require their target environments. H2 checks are not SQL Server compatibility evidence.
