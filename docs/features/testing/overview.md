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
| Admin user role / exam level / delete guards | `AuthServiceTest` |
| Logged-in password change | `AuthServiceTest` |
| Seed subject visible on both exam levels | `SubjectDaoTest` |
| Attempt `isDiagnostic` joined from exam | `AttemptDaoTest` |
| Suspect leave flag rules | `BehaviorIntegrityTest` |
| Leave ingest, intro ignore, summary | `BehaviorTrackingServiceTest` |
| Import creates leveled subjects | `QuestionImportServiceTest` |
| Import rejects both-false level flags | `QuestionImportServiceTest` |
| Import upsert / template / export round-trip | `QuestionImportServiceTest` |
| Week clock / quotas / official lock / missed week | `WeekClockTest`, `QuestionSamplerTest`, `WeeklyRegimenServiceTest` |
| Tagalog / English message bundles | `MessagesTest`, `LocaleSupportTest` |
