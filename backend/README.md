# KEYSTONE — Backend (Spring Boot + MySQL)

Field Service Management Platform backend for Project KEYSTONE. Spring Boot 3
(Java 21), **MySQL**, Flyway-managed schema, stateless JWT auth, and the
governed work-order lifecycle described in the engineering brief.

> **Note on how this was built:** this code was written and reviewed carefully
> but **could not be compiled inside this sandbox** — no network access to
> Maven Central here, so `mvn`/Eclipse's Maven integration can't download
> Spring Boot, JJWT, etc. in this environment. Import it into Eclipse (steps
> below) and let Eclipse/Maven resolve dependencies on your machine — send me
> any errors that come up and I'll fix them fast.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (Web, Validation, Security) |
| Persistence | Spring Data JPA / Hibernate |
| Database | **MySQL 8.0.16+** (needed for `CHECK` constraint enforcement) |
| Migrations | Flyway (`flyway-mysql` module) |
| Auth | Spring Security + JWT (jjwt) |
| API docs | springdoc-openapi (Swagger UI) |
| Boilerplate | Lombok |

## 1. Prerequisites

- **Java 21 JDK**
- **Eclipse IDE for Enterprise Java and Web Developers** (this bundle includes
  the Maven (m2e) and Java EE tooling you need — plain "Eclipse IDE for Java
  Developers" also works but may need the m2e plugin added manually)
- **MySQL 8** running locally, or Docker to run it in a container
- **Lombok** — see step 3, this needs a one-time install into Eclipse itself

## 2. Set up MySQL

**Option A — Docker (easiest):**
```bash
docker compose up -d
```
This starts MySQL on `localhost:3306` with database `keystone`, user
`keystone`, password `keystone` (see `docker-compose.yml`).

**Option B — local MySQL install:**
```sql
CREATE DATABASE keystone CHARACTER SET utf8mb4;
CREATE USER 'keystone'@'localhost' IDENTIFIED BY 'keystone';
GRANT ALL PRIVILEGES ON keystone.* TO 'keystone'@'localhost';
FLUSH PRIVILEGES;
```

## 3. Install the Lombok plugin into Eclipse (one-time, important)

This project uses Lombok (`@Getter`, `@Setter`, `@Builder`, etc.) to keep the
entities and DTOs compact. Eclipse doesn't understand these annotations out of
the box — you'll get red errors on every entity until Lombok is installed
**into Eclipse itself** (not just added as a Maven dependency, which it already
is in `pom.xml`).

1. Download `lombok.jar` from https://projectlombok.org/download (or find it
   already in your local Maven repo at
   `~/.m2/repository/org/projectlombok/lombok/1.18.34/lombok-1.18.34.jar`
   after the first Maven build attempt)
2. Run it: `java -jar lombok.jar`
3. It auto-detects your Eclipse installation — tick the box next to it and
   click **Install / Update**
4. **Restart Eclipse**

If Eclipse still shows errors on Lombok-generated methods afterward, go to
`eclipse.ini` (in your Eclipse install folder) and confirm a line like
`-javaagent:lombok.jar` was added near the top — the installer usually does
this automatically.

## 4. Import the project into Eclipse

1. Unzip `keystone-backend.zip` somewhere on disk
2. Open Eclipse
3. `File → Import...`
4. Choose **Maven → Existing Maven Projects** → **Next**
5. **Root Directory** → Browse to the unzipped `keystone-backend` folder
6. Eclipse will detect `pom.xml` and show the project checked in the list — click **Finish**
7. Eclipse will now download all dependencies (Spring Boot, JJWT, MySQL
   driver, etc.) — this can take a few minutes on first import. Watch the
   bottom-right progress bar.
8. Once it settles, right-click the project → **Maven → Update Project...**
   (tick "Force Update") if you still see red errors

## 5. Run it from Eclipse

1. In the **Project Explorer**, expand
   `src/main/java` → `com.zidio.keystone` → `KeystoneApplication.java`
2. Right-click it → **Run As → Java Application**
   (or **Spring Boot App** if you have Spring Tools installed as an Eclipse
   add-on — either works identically here)
3. Watch the **Console** view — on first boot, Flyway runs the two migrations
   in `src/main/resources/db/migration/`:
   - `V1__init_schema.sql` — creates all tables
   - `V2__seed_data.sql` — seeds 2 customers, 3 sites, 3 parts, 4 users (one
     per role), and 3 sample work orders in different lifecycle states
4. Once you see `Started KeystoneApplication in X seconds`, the API is live at
   `http://localhost:8080`

## 6. Seed logins

All seed users share the password **`Password123!`**

| Role | Email |
|---|---|
| Dispatcher | `dispatcher@keystone.dev` |
| Technician | `technician@keystone.dev` |
| Manager | `manager@keystone.dev` |
| Customer | `customer@keystone.dev` |

Try it (from a terminal, or Postman, or Eclipse's built-in nothing-fancy — curl is simplest):
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@keystone.dev","password":"Password123!"}'
```
You'll get back `{ "token": "...", "user": {...} }`. Use the token as
`Authorization: Bearer <token>` on every other request.

## 7. API docs

Swagger UI: **http://localhost:8080/swagger-ui.html**
Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 8. Environment variables (for anything beyond local dev)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/keystone?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL |
| `DB_USERNAME` | `keystone` | DB user |
| `DB_PASSWORD` | `keystone` | DB password |
| `JWT_SECRET` | (dev default in `application.yml`) | Base64, 256-bit+. **Change this for anything beyond local dev.** Generate with `openssl rand -base64 32` |
| `JWT_EXPIRATION_MINUTES` | `480` | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed to call the API (set to your deployed frontend URL in production) |
| `SERVER_PORT` | `8080` | — |

To set these in Eclipse instead of via shell env vars: right-click
`KeystoneApplication.java` → **Run As → Run Configurations... → Environment
tab → Add** to set any of the above per-run.

Never commit real secrets — `.gitignore` already excludes `.env`.

## 9. Common Eclipse troubleshooting

| Symptom | Fix |
|---|---|
| Red squiggles on every `@Getter`/`@Builder` usage | Lombok isn't installed into Eclipse — see step 3 |
| "Project has no default constructor" or similar Lombok-related errors persist after install | Restart Eclipse fully (not just close/reopen the workspace) |
| Maven dependencies not resolving / red X on the project | Right-click project → **Maven → Update Project...**, tick "Force Update of Snapshots/Releases" |
| `Access denied for user 'keystone'@'localhost'` | Confirm MySQL is running and the user/password/grants from step 2 were applied — check with `mysql -u keystone -p keystone` |
| `Public Key Retrieval is not allowed` | Already handled by `allowPublicKeyRetrieval=true` in the JDBC URL above — if you changed the URL, keep that flag |
| Flyway checksum mismatch on a later run | You edited an already-applied migration file — don't; add a new `V3__...sql` instead, or wipe the dev DB and restart |

## 10. How the pieces map to the brief

### The lifecycle (Section 07) — `WorkOrderStatus.java` + `WorkOrderService`
`WorkOrderStatus.ALLOWED_TRANSITIONS` is the single source of truth for legal
jumps. `WorkOrderService.transitionStatus()` checks it and throws
`InvalidTransitionException` (→ HTTP 409) on anything illegal. Role
restrictions per transition (e.g. "only a manager can CLOSE") live in
`assertRoleCanPerformTransition()`. Every transition writes an append-only
`WorkOrderStatusHistory` row.

### Security (Section 08) — `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`
Stateless JWT, BCrypt passwords, `@PreAuthorize` on every service method (not
just the controller — defense in depth). `WorkOrderService` additionally
scopes every query and single-record fetch by role: a technician's list query
is filtered to `assignedTo = me` **in the SQL**, not after the fact; a
customer's queries are filtered to their own `customerId`. This is the actual
security boundary — the frontend's route guards are UX only.

### Transactional integrity (Section 05/06) — `WorkOrderService.logPartUsage()`
Stock check + decrement + usage-log insert all happen inside one
`@Transactional` method, so a failure anywhere rolls back the whole thing.
Stock is also protected at the DB level with a `CHECK (stock_qty >= 0)`
constraint as a second line of defense.

### SLA tracking (F7) — `WorkOrderService.computeSlaState()` + `SlaMonitorScheduler`
SLA due date is set at creation based on priority (configurable in
`application.yml` under `keystone.sla.hours.*`). State (`OK` / `AT_RISK` /
`BREACHED`) is computed on read, not stored, so it's always current.
`SlaMonitorScheduler` runs every 5 minutes and logs breaches — swap the `TODO`
for a real notification (email/in-app) when you're ready to wire one up.

### Dashboard (F8) — `ReportService` / `GET /api/reports/summary`
Manager-only. Counts by status, overdue count, a compliance rate, and load by
technician — shapes match the frontend's `DashboardSummary` type exactly.

## 11. Project structure

```
src/main/java/com/zidio/keystone/
  config/        SecurityConfig, OpenApiConfig
  security/      JWT service/filter, UserDetails adapter
  domain/        JPA entities + enums (Role, Priority, WorkOrderStatus, SlaState)
  repository/    Spring Data JPA repositories
  dto/           Request/response records — entities never serialise directly
  service/       Business logic, the state machine, RBAC checks
  controller/    Thin REST controllers — no business logic
  exception/     Custom exceptions + a global handler for consistent error shapes
src/main/resources/
  application.yml
  db/migration/  Flyway scripts (V1 schema, V2 seed data)
src/test/        WorkOrderStatusTest — lifecycle transition unit tests
```

## 12. What's next / left as a starting point

- **More tests.** Section 16.1 flags the lifecycle and authorisation rules as
  the highest-value things to cover. `WorkOrderStatusTest` covers the pure
  state machine; add `@SpringBootTest` integration tests next for the
  cross-customer/cross-technician access-denial cases (a customer hitting
  another customer's work order by ID, a technician trying to close a job,
  etc.) — those are exactly what a reviewer will try first.
- **Notifications.** `SlaMonitorScheduler` logs breaches; wire in real email
  or an in-app notifications table when ready.
- **A dedicated `closed_at` column.** `ReportService` currently uses
  `updated_at` as a proxy for "when a work order was closed" to compute SLA
  compliance — accurate in practice (closing is usually the last write) but a
  purpose-built column would be cleaner.
- **User management endpoints.** Manager-only user/technician CRUD isn't
  built yet — seed data covers the 4 demo logins; add `POST /api/users` etc.
  when you need to onboard real users beyond the seed set.
