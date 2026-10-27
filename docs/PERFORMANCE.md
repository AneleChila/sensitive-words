# Performance baseline

## Reproducible workload

The required baseline must be run on the deployment target after starting MSSQL and the API. Record CPU architecture, CPU count, memory, JDK, SQL Server image digest, dictionary size, message lengths, concurrency, throughput, p50/p95/p99 latency, and SQL query count. A representative matrix is 100/1,000/10,000-character messages, 228/1,000/10,000 terms, and concurrency 1/10/50.

No benchmark result is claimed in this repository because the current environment has not completed that controlled workload. Correctness limits are 10,000 message characters, 10,000 import entries, 1 MiB imports, and a ten-connection pool.

## Current design and proposals

Filtering performs one committed dictionary query and compiles one matcher per request. Import fetches existing terms in one set query and writes a batch, avoiding a duplicate query per entry. Pagination is database-bounded.

A cached immutable compiled matcher is a proposed optimization, not implemented. It would require after-commit invalidation, a monotonically increasing dictionary version shared across replicas, a defined freshness bound, and fail-closed behavior when refresh fails. A trie or Aho-Corasick matcher becomes worthwhile only when measured regex compilation or matching dominates latency at larger dictionaries. Rate limiting, pool tuning, and horizontal scaling remain deployment controls that must be sized from measured traffic.
