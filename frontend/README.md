# KEYSTONE — Frontend (React + TypeScript)

Field-service platform front end for Project KEYSTONE, covering all four roles:
Dispatcher, Technician, Manager, Customer.

## Stack
- React 18 + TypeScript, built with Vite
- react-router-dom v6 for routing
- axios for API calls, with a JWT interceptor
- Leaflet / react-leaflet for the tracking map (OpenStreetMap tiles, no API key)
- No UI framework — hand-rolled CSS with design tokens (`index.css`)

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
  api/            axios client + one module per resource (auth, workOrders,
                  customers, technicians, map)
  chatbot/        rule-based responder for the customer support widget —
                  no external API, answers only from the caller's own
                  already-fetched work orders
  components/     Layout (role-aware sidebar), ProtectedRoute, badges/chips,
                  SupportChatbot, AttachmentGallery
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

Other routes: `/new-request` (raise/create a work order — customer and
staff), `/technicians` (Manager only — roster management), `/map` (Manager
and Customer — tracking map), `/forgot-password` / `/reset-password`,
`/demo-login` (a role picker for quick demo logins, not linked from the main
nav).

## Important: this is a UX convenience, not the security boundary

`ProtectedRoute` and the sidebar only hide navigation options — they do **not**
protect data. Every API call still goes through JWT auth and must be
re-checked server-side (Spring Security `@PreAuthorize`). If a customer's
token is used to call `/api/work-orders/{id}` for another customer's job, the
**server** must reject it — assume the frontend gate can always be bypassed by
calling the API directly.

## Backend endpoints this expects

- `POST /api/auth/login` → `{ token, user }`
- `POST /api/auth/register`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`
- `GET  /api/work-orders?status=&q=&page=&size=` → paginated, role-scoped
- `GET  /api/work-orders/{id}` → includes history, parts, time logs, attachments
- `POST /api/work-orders` → create
- `POST /api/work-orders/{id}/assign`
- `POST /api/work-orders/{id}/status` → `{ toStatus, note }`, 409 on illegal transition
- `POST /api/work-orders/{id}/parts` → `{ partId, qtyUsed }`
- `POST /api/work-orders/{id}/time` → `{ minutes, note }`
- `POST /api/work-orders/{id}/attachments` (multipart), `GET .../attachments/{id}`
- `GET  /api/work-orders/{id}/nearest-technicians` → distance-sorted for the assign picker
- `GET  /api/reports/summary` → dashboard metrics
- `GET  /api/customers`, `GET/POST /api/customers/{id}/sites`, `GET /api/customers/me/sites`
- `GET  /api/users/technicians`, `POST /api/users/technicians`, `POST /api/users/technicians/{id}/base`
  — **`POST`, not `PATCH`**, for the base-address update: Render's production
  edge was observed dropping `PATCH` requests even though the identical route
  works for every other verb. Don't reintroduce `PATCH` here without
  re-verifying against the live deployment first.
- `GET  /api/map/overview` (Manager), `GET /api/map/my-requests` (Customer)

## What's deliberately left as a starting point

- The status-transition buttons in `WorkOrderDetail.tsx` are an optimistic
  client-side mirror of the backend's state machine — the source of truth is
  the 409 the server returns on an illegal jump.
- The support chatbot is intentionally narrow (raise a request / check a
  request / SLA explanation / lifecycle explanation) — extending its intent
  matching lives entirely in `chatbot/responder.ts`, no backend change needed.
- Live GPS tracking would replace the current static, manager-set
  home-base-address model in `TrackingMap.tsx` / the map API.
- No frontend test suite yet — the backend has real integration test coverage
  (`MapAndTechnicianIntegrationTest`); consider Playwright for the frontend
  next, matching that same "real HTTP, real data" philosophy rather than
  component-level mocks.
