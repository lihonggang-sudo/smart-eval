# smart-eval

高校处级干部测评系统。

## Documents

- Design: `docs/superpowers/specs/2026-07-03-smart-eval-design.md`
- Implementation plan: `docs/superpowers/plans/2026-07-03-smart-eval-implementation.md`

## Services

- Backend: `backend/README.md`
- Frontend: `frontend/README.md`

## Local Development

This environment uses Java 17 for the backend.

```bash
docker compose up -d postgres
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

The Vite dev server proxies `/api` requests to the backend at `http://localhost:8080`.
Administrator bootstrap is disabled by default. For a clean local database, enable it explicitly for the first backend startup with `APP_BOOTSTRAP_ADMIN_ENABLED=true`, `APP_BOOTSTRAP_ADMIN_USERNAME=admin`, and `APP_BOOTSTRAP_ADMIN_PASSWORD=<local-password>`, then disable it after creating the first administrator.

## Frontend E2E

From `frontend/`:

```bash
npm exec playwright install
npm exec playwright test
```

The current Playwright spec is route-rendering only. It checks `/admin/login` and `/mobile/login` without requiring seeded credentials.
