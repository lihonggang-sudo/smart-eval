# Smart Eval Frontend

## Requirements

- Node.js 20+
- npm 10+

## Install

```bash
npm install
```

## Run

```bash
npm run dev
```

The app listens on `http://localhost:5173`.
During local development, `/api` requests are proxied to `http://localhost:8080`.

Administrator routes start at `/admin`.
Mobile evaluator routes start at `/mobile/login`.

## Browser E2E

Install Playwright browser binaries when they are not already present:

```bash
npm exec playwright install
```

Run the route-rendering e2e check:

```bash
npm exec playwright test
```

This e2e coverage only verifies that the `/admin/login` and `/mobile/login` routes render. It does not require seeded credentials or backend login data.
