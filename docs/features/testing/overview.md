# Automated tests

Unit/integration tests live under `src/test/java` and run with JUnit 5 via Maven Surefire.

```bash
mvn test
```

CI (`.github/workflows/ci.yml`) runs `mvn -B package`, which includes tests.

## Approach

- `DatabaseManager.reinitForTesting()` spins up an isolated in-memory H2 database and re-runs `schema.sql` (including seed subjects/exams).
- `DatabaseTestSupport` resets the DB before each test method.

## Coverage focus (High-priority flows)

| Area | Test class |
|------|------------|
| Access grant create / redeem / revoke / active access | `AccessGrantServiceTest` |
| Seed subject visible on both exam levels | `SubjectDaoTest` |
| Attempt `isDiagnostic` joined from exam | `AttemptDaoTest` |
| Import creates leveled subjects | `QuestionImportServiceTest` |
