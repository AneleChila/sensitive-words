# Performance considerations

## Reproducible workload

Run the proposed baseline against MSSQL and the API in a controlled development or staging environment. Record CPU architecture, CPU count, memory, JDK, SQL Server image digest, dictionary size, message lengths, concurrency, throughput, p50/p95/p99 latency, and SQL query count. A representative matrix is 100/1,000/10,000-character messages, 228/1,000/10,000 terms, and concurrency 1/10/50. Use a disposable database for dictionary-size changes. Repeat relevant workloads on the intended deployment environment before making capacity commitments.

Controlled benchmarks have not been run; no throughput or latency results are claimed. Configured limits are 10,000 UTF-16 code units per message, 10,000 import entries, 1 MiB per import file, and a ten-connection pool.

## Current design and proposals

Filtering performs one committed dictionary query and compiles one matcher per request. Import checks existing terms in groups of at most 1,000 parameters, staying below SQL Server's request parameter limit and avoiding a duplicate-check query for every entry. New entries are saved in one transaction. `saveAll` alone does not configure JDBC insert batching; batch size and persistence-context flushing would need separate tuning and measurement. Pagination is database-bounded.

A cached immutable compiled matcher is a proposed optimization, not implemented. It would require after-commit invalidation, a monotonically increasing dictionary version shared across replicas, a defined freshness bound, and fail-closed behavior when refresh fails. A trie or Aho-Corasick matcher becomes worthwhile only when measured regex compilation or matching dominates latency at larger dictionaries. Rate limiting, pool tuning, and horizontal scaling remain deployment controls that must be sized from measured traffic.
