# Validation record

Validated on 21 October 2026 with OpenJDK 17.0.20, Maven 3.9.11 and Spring Boot 3.5.16.

## Automated build

`mvn -B verify` completed successfully. Final result: **22 tests, 0 failures, 0 errors, 0 skipped**.

| Suite | Tests | Coverage |
|---|---:|---|
| MessageMaskerTest | 8 | Case, Unicode boundaries, phrase priority, literal regex characters, repeated terms, empty dictionary/message, supplementary characters |
| ApiIntegrationTest | 7 | Flyway/H2, CRUD/filter lifecycle, normalization/duplicates, invalid requests, empty messages, authorization, pagination |
| ProductionSecurityTest | 3 | JWT scope separation, invalid/missing bearer tokens, demo Basic auth disabled |
| JwtValidationTest | 4 | Valid claims, wrong audience, wrong issuer, expired token |

Production routing tests stub the decoder. Claim validation tests exercise the configured validator directly. Neither test contacts a real identity provider or proves an external token signing configuration.

## Packaged application smoke test

Started the executable JAR using the `local` profile on a temporary HTTP port, then verified:

* Readiness returns HTTP 200.
* A fresh database contains exactly 228 dictionary entries.
* `SELECT * FROM customers; DROP TABLE customers` returns `****** * **** customers; **** ***** customers`.
* The client demo user receives HTTP 403 on internal dictionary access.
* The process shuts down after testing.

Also parsed the OpenAPI YAML, resolved every local `$ref`, and parsed the Postman JSON. The delivered JAR archive was checked for integrity before packaging.

## Not executed here

Docker image build, Docker Compose, real PostgreSQL, real OIDC discovery/signature verification, infrastructure provisioning, dependency/container security scanning, performance/load tests, failover and backup restore. Follow the staging procedure in `DEPLOYMENT.md` before production use. H2's compatibility mode is a local test convenience, not evidence of PostgreSQL integration.
