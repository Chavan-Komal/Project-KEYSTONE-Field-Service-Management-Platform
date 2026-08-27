# KEYSTONE — Frontend (React + TypeScript)

Field-service platform front end for Project KEYSTONE, covering all four roles:
Dispatcher, Technician, Manager, Customer.

## Stack
- React 18 + TypeScript, built with Vite
- react-router-dom v6 for routing
- axios for API calls, with a JWT interceptor

## Getting started

```bash
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api/*` to
`http://localhost:8080` (your Spring Boot backend) — see `vite.config.ts`.

Build for production:
```bash
npm run build
```
Output goes to `dist/`.

## How it's structured

```
src/
  api/            axios client + one module per resource (auth, workOrders)
  components/     Layout (role-aware sidebar), ProtectedRoute, badges/chips
  context/        AuthContext — holds the JWT + logged-in user
  pages/          one file per screen
  types/          shared TypeScript types mirroring the domain model
```

## Role → landing page

| Role       | Lands on         | Route          |
|------------|------------------|----------------|
| Dispatcher | Work Order Board | `/board`       |
| Technician | My Jobs          | `/my-jobs`     |
| Manager    | Dashboard        | `/dashboard`   |
| Customer   | My Requests      | `/portal`      |

`Home.tsx` redirects `/` to the right screen based on `user.role`.

## Important: this is a UX convenience, not the security boundary

`ProtectedRoute` and the sidebar only hide navigation options — they do **not**
protect data. Every API call still goes through JWT auth and must be
re-checked server-side (Spring Security `@PreAuthorize`), per Section 03/08 of
the brief. If a customer's token is used to call `/api/work-orders/{id}` for
another customer's job, the **server** must reject it — assume the frontend
gate can always be bypassed by calling the API directly.

## Backend endpoints this expects (Section 10 / Appendix B)

- `POST /api/auth/login` → `{ token, user }`
- `GET  /api/work-orders?status=&q=&page=&size=` → paginated, role-scoped
- `GET  /api/work-orders/{id}` → includes history, parts, time logs
- `POST /api/work-orders` → create
- `POST /api/work-orders/{id}/assign`
- `POST /api/work-orders/{id}/status` → `{ toStatus, note }`, 409 on illegal transition
- `POST /api/work-orders/{id}/parts` → `{ partId, qtyUsed }`
- `POST /api/work-orders/{id}/time` → `{ minutes, note }`
- `GET  /api/reports/summary` → dashboard metrics

Swap the mock shapes in `src/types/index.ts` for whatever your DTOs actually
return once the backend is up — everything else consumes those types, so
that's the one place to adjust.

## What's deliberately left as a starting point

- The status-transition buttons in `WorkOrderDetail.tsx` are an optimistic
  client-side mirror of the diagram in Section 07 — the source of truth is
  the 409 the server returns on an illegal jump.
- Customer/site pickers on `NewWorkOrder.tsx` are plain ID inputs; swap for
  a real autocomplete once the `/api/customers` and `/api/sites` endpoints exist.
- No test suite yet — the brief calls out lifecycle transitions and
  authorisation rules as the highest-value things to cover (Section 16.1).
