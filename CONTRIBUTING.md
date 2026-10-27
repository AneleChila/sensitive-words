# Contributing

Create changes on a feature branch and keep commits focused. Review the staged diff before every commit.

Every commit subject must start with the issue reference, followed by a Conventional Commit type, optional scope, and a concise description:

```text
[#1] feat(database): configure MSSQL and Flyway migrations
[#3][#6] test(import): verify rollback on invalid entries
```

Use only the issue numbers addressed by that commit. Run the relevant tests before committing and `./mvnw verify` before opening a pull request. Never commit credentials, `.env`, IDE metadata, or build output.
