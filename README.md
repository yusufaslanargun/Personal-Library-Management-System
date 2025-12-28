# Personal Library Management System (PLMS)

Full-stack PLMS built with Java 21 + Spring Boot 3 + PostgreSQL + React.

## Quick Start (Docker)

1) Copy env:

```bash
cp .env.example .env
```

2) Build + run:

```bash
docker compose up --build
```

Services:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Web: http://localhost:5173

## Local Development

Backend:

```bash
mvn -f backend/pom.xml spring-boot:run
```

Frontend:

```bash
npm --prefix frontend install
npm --prefix frontend run dev
```

## Migrations
Flyway runs automatically on startup. SQL files are in `backend/src/main/resources/db/migration`.

## Seed (5000 items)

```bash
DATABASE_URL=postgresql://plms:plms@localhost:5432/plms ./scripts/seed.sh
```

## Import / Export

- Export JSON: `GET /export?format=json`
- Export CSV (zip): `GET /export?format=csv`
- Import JSON: `POST /import?format=json` (multipart file)
- Import CSV: `POST /import?format=csv` (multipart file)

## Tests

Backend unit + integration tests (Testcontainers Postgres):

```bash
mvn -f backend/pom.xml test
```

E2E (Playwright):

```bash
PLMS_EXTERNAL_MOCK=true npm --prefix frontend run test:e2e
```

Run API + Web before E2E (or use docker compose).

## Environment Variables

See `.env.example` for all configuration, including API keys and sync settings.
Set `PLMS_EXTERNAL_MOCK=true` to run without external API calls (useful for tests).

## Documentation

- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/SECURITY.md`
- `docs/PERF.md`
- `docs/CHECKLIST.md`
