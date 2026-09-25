# KEYSTONE — Backend (Spring Boot + PostgreSQL)

Field Service Management Platform backend for Project KEYSTONE. Spring Boot 3
(Java 21), **PostgreSQL**, Flyway-managed schema, stateless JWT auth, and the
governed work-order lifecycle described in the engineering brief — plus a set
of features built beyond the original brief (see Section 10).

Deployed and live: the backend runs on Render, the frontend on Netlify.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (Web, Validation, Security) |
| Persistence | Spring Data JPA / Hibernate |
| Database | **PostgreSQL 13+** |
| Migrations | Flyway (`flyway-database-postgresql` module) — `V1`–`V4` |
| Auth | Spring Security + JWT (jjwt), stateless |
| API docs | springdoc-openapi (Swagger UI) |
| Geocoding | OpenStreetMap Nominatim (free, no API key) |
| Testing | JUnit 5 + Spring Boot Test, real integration tests against an embedded (native, no-Docker-required) Postgres |

No Lombok — entities and DTOs use hand-written getters/setters and builders.

## 1. Prerequisites

- **Java 21 JDK**
- **Maven** (or use your IDE's bundled Maven integration — Eclipse's m2e works fine)
- **PostgreSQL 13+** running locally, or Docker to run it in a container
- If Docker isn't available: `io.zonky.test:embedded-postgres` (already a test
  dependency) can run a real, native Postgres binary with zero Docker — see
  `MapAndTechnicianIntegrationTest` for how the test suite uses it. The same
  trick works for local dev: `initdb` + `pg_ctl start -o "-p 5432"` against a
  scratch data directory gets you a real local Postgres without Docker at all.

## 2. Set up PostgreSQL

**Option A — Docker (easiest):**
```bash
docker compose up -d
```
This starts PostgreSQL on `localhost:5432` with database `keystone`, user
`keystone`, password `keystone` (see `docker-compose.yml`).

**Option B — local PostgreSQL install:**
```sql
CREATE DATABASE keystone;
CREATE USER keystone WITH PASSWORD 'keystone';
GRANT ALL PRIVILEGES ON DATABASE keystone TO keystone;
```

## 3. Import the project into Eclipse (or any Maven-aware IDE)

1. Open Eclipse
2. `File → Import...`
3. Choose **Maven → Existing Maven Projects** → **Next**
4. **Root Directory** → browse to the `backend` folder
5. Eclipse detects `pom.xml` and shows the project checked in the list — click **Finish**
6. Eclipse downloads all dependencies (Spring Boot, JJWT, Postgres driver,
   Flyway, springdoc, etc.) — takes a few minutes on first import
7. If you still see red errors afterward: right-click the project → **Maven →
   Update Project...** (tick "Force Update")

## 4. Run it

**From Eclipse:** expand `src/main/java` → `com.zidio.keystone` →
`KeystoneApplication.java` → right-click → **Run As → Java Application** (or
**Spring Boot App** if you have Spring Tools installed).

**From the command line:**
```bash
mvn spring-boot:run
```

On first boot, Flyway runs every migration in `src/main/resources/db/migration/`:
- `V1__init_schema.sql` — creates all tables
- `V2__seed_data.sql` — seeds 2 customers, 3 sites, 3 parts, 4 users (one per
  role), 3 sample work orders in different lifecycle states
- `V3__password_reset_and_attachments.sql` — password reset tokens, work
  order photo attachments
- `V4__technician_locations_and_geocoding.sql` — lat/lng on sites and a
  technician home-base address, with seed backfill

Once you see `Started KeystoneApplication in X seconds`, the API is live at
`http://localhost:8080`.

## 5. Seed logins

All seed users share the password **`Password123!`**

| Role | Email |
|---|---|
| Dispatcher | `dispatcher@keystone.dev` |
| Technician | `technician@keystone.dev` |
| Manager | `manager@keystone.dev` |
| Customer | `customer@keystone.dev` |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@keystone.dev","password":"Password123!"}'
```
You'll get back `{ "token": "...", "user": {...} }`. Use the token as
`Authorization: Bearer <token>` on every other request.

## 6. API docs

Swagger UI: **http://localhost:8080/swagger-ui.html**
Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 7. Environment variables (for anything beyond local dev)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/keystone` | JDBC URL |
| `DB_USERNAME` | `keystone` | DB user |
| `DB_PASSWORD` | `keystone` | DB password |
| `JWT_SECRET` | (dev default in `application.yml`) | Base64, 256-bit+. **Change this for anything beyond local dev.** `openssl rand -base64 32` |
| `JWT_EXPIRATION_MINUTES` | `480` | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated origins allowed to call the API |
| `SERVER_PORT` / `PORT` | `8080` | Render injects `PORT`; `SERVER_PORT` is the fallback everywhere else |
| `FRONTEND_BASE_URL` | `http://localhost:5173` | Where the emailed password-reset link points |
| `RESET_TOKEN_TTL_MINUTES` | `30` | Password-reset token lifetime |
| `EXPOSE_RESET_TOKEN` | `true` | If true, `/api/auth/forgot-password` also returns the reset link in the response body — convenient for dev, **set to `false` in production** so the link only goes by email |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | unset | Configure to send real password-reset emails; with none set, the link is just logged |
| `ATTACHMENT_MAX_BYTES` | `5242880` (5MB) | Work order photo upload cap |

Never commit real secrets — `.gitignore` already excludes `.env`.

## 8. Common troubleshooting

| Symptom | Fix |
|---|---|
| Maven dependencies not resolving / red X on the project | Right-click project → **Maven → Update Project...**, tick "Force Update of Snapshots/Releases" |
| `password authentication failed for user "keystone"` | Confirm PostgreSQL is running and the user/password/grants from step 2 were applied |
| Flyway checksum mismatch on a later run | You edited an already-applied migration file — don't; add a new `V5__...sql` instead, or wipe the dev DB and restart |
| A `PATCH` request works locally but 404s once deployed | Render's edge (Cloudflare) has been observed dropping `PATCH` in production even when the identical route works for `POST` — this project avoids `PATCH` entirely for that reason (see `UserController.updateTechnicianBase`) |
| Docker unavailable for local Postgres | Use the embedded-postgres approach from Section 1 — no Docker needed |

## 9. How the pieces map to the brief

### The lifecycle (Section 07) — `WorkOrderStatus.java` + `WorkOrderService`
`WorkOrderStatus.ALLOWED_TRANSITIONS` is the single source of truth for legal
jumps. `WorkOrderService.transitionStatus()` checks it and throws
`InvalidTransitionException` (→ HTTP 409) on anything illegal. Every
transition writes an append-only `WorkOrderStatusHistory` row.

### Security (Section 08) — `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`
Stateless JWT, BCrypt passwords, `@PreAuthorize` on every service method (not
just the controller — defense in depth). Every list/read query is scoped by
role **in the SQL** via `WorkOrderSpecifications` — a technician's query has
`assignedTo = me` baked into the `WHERE` clause, a customer's has their own
`customerId`. That's the real security boundary; the frontend's route guards
are UX only.

### Transactional integrity (Section 05/06) — `WorkOrderService.logPartUsage()`
Stock check + decrement + usage-log insert all happen inside one
`@Transactional` method. Stock is also protected at the DB level with a
`CHECK (stock_qty >= 0)` constraint as a second line of defense.

### SLA tracking (F7) — `WorkOrderService.computeSlaState()`
SLA due date is set at creation based on priority (configurable under
`keystone.sla.hours.*`). State (`OK` / `AT_RISK` / `BREACHED`) is computed on
read, not stored, so it's always current.

### Dashboard (F8) — `ReportService` / `GET /api/reports/summary`
Manager-only. Counts by status, overdue count, a compliance rate, load by
technician.

## 10. Built beyond the original brief

- **Self-service password reset** — `AuthController`/`AuthService`, token-based, `V3` migration
- **Work order photo attachments** — multipart upload, image-only, size-capped, `V3` migration
- **Manager-provisioned technician roster** — `UserService.createTechnician()`,
  staff accounts are never self-registered (Section 03)
- **Geocoding** — `GeocodingService` calls OpenStreetMap's free Nominatim API;
  best-effort, never blocks a write if the external call fails
- **Nearest-technician dispatch suggestion** — `WorkOrderService.nearestTechnicians()`,
  haversine distance (`util/GeoMath`) from each technician's geocoded base to
  the job's geocoded site
- **Tracking map data** — `MapService`/`MapController`, org-wide for Manager,
  own-sites-only for Customer
- **Customer-scoped site picker** — `GET /api/customers/me/sites` resolves the
  caller's own organisation from the JWT, replacing a staff-only endpoint that
  used to accept any customer ID from any authenticated caller

## 11. Project structure

```
src/main/java/com/zidio/keystone/
  config/        SecurityConfig
  security/      JWT service/filter, UserDetails adapter
  domain/        JPA entities + enums
  repository/    Spring Data JPA repositories
  dto/           Request/response records — entities never serialise directly
  service/       Business logic, the state machine, RBAC checks, geocoding, map
  controller/    Thin REST controllers — no business logic
  exception/     Custom exceptions + a global handler for consistent error shapes
  util/          GeoMath (haversine distance)
src/main/resources/
  application.yml
  db/migration/  Flyway scripts, V1 through V4
src/test/        WorkOrderStatusTest (lifecycle unit tests),
                 MapAndTechnicianIntegrationTest (real end-to-end HTTP tests
                 against a real embedded Postgres — roster, nearest-tech,
                 map endpoints, and role-based access denial)
```

## 12. What's next

- **Live GPS tracking** for technicians in the field, instead of a static home-base address
- **WebSocket/SSE push** for status changes, instead of client-side polling
- **Dispatcher access to the tracking map** — currently Manager and Customer only
- **Rate-limit/cache geocoding calls** ahead of any real Nominatim usage-policy limits
- **A dedicated `closed_at` column** — `ReportService` currently uses `updated_at`
  as a proxy for "when a work order was closed," accurate in practice but a
  purpose-built column would be cleaner
