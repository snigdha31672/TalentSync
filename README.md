# Online Job Recruitment System — Working Build (Review 1)

Review 1's official rubric grades Problem Analysis & Requirement Specification,
Microservice Identification & Service Discovery, **JWT Authentication**, and
**API Gateway Configuration** — so unlike an earlier draft of this skeleton, JWT
auth and the gateway are implemented and working here, not deferred. Business
CRUD logic is real (backed by PostgreSQL via Spring Data JPA), not stubbed.

## Modules

| Module | Port | Purpose |
|---|---|---|
| `common` | — | Shared JWT util (library, not a running service) |
| `eureka-server` | 8761 | Service discovery registry |
| `auth-service` | 8084 | Register/login; issues JWTs |
| `api-gateway` | 8080 | Single entry point; validates JWTs; routes to the four services |
| `job-service` | 8081 | Job postings (`job` table) |
| `application-service` | 8082 | Candidate applications (`application` table) |
| `recruitment-service` | 8083 | Interview/recruitment tracking (`recruitment` table) |

## What's real vs. still coming

- **Real:** full CRUD in every service (backed by Postgres), JWT issuance
  (`auth-service`) and validation (gateway + every service independently),
  role-based authorization (`CANDIDATE` / `RECRUITER`) matching the API design's
  Auth column, Eureka registration, gateway routing + CORS.
- **Still simplified (fine for Review 1, worth hardening later):** no
  ownership checks (e.g. a candidate can `GET` any application by ID, not only
  their own), no refresh tokens, no OpenFeign inter-service calls yet, no rate
  limiting/circuit breakers on the gateway, JWT secret is a plain string in
  `application.yml` rather than a vault/env-var secret.

## Prerequisites

- JDK 17+, Maven 3.8+
- PostgreSQL running locally, with pgAdmin (or `psql`) to create four databases

## 1. Create the databases

Open pgAdmin's Query Tool against your default `postgres` database (or use
`psql -U postgres`) and run `create_databases.sql` from this repo root — it
just does:

```sql
CREATE DATABASE job_db;
CREATE DATABASE application_db;
CREATE DATABASE recruitment_db;
CREATE DATABASE auth_db;
```

Each `application.yml` assumes user `postgres` / password `postgres` on
`localhost:5432` — edit `spring.datasource.*` in each service if yours differ.

## 2. Build

```bash
mvn -q clean install
```

`common` builds first automatically (Maven resolves module order from the
dependency graph), so `auth-service`, `api-gateway`, and the three business
services can all depend on it.

## 3. Run order

Eureka first, then auth + the three business services (any order among
those four), then the gateway last so it can resolve already-registered
service names.

```bash
cd eureka-server        && mvn spring-boot:run
cd auth-service         && mvn spring-boot:run
cd job-service          && mvn spring-boot:run
cd application-service  && mvn spring-boot:run
cd recruitment-service  && mvn spring-boot:run
cd api-gateway          && mvn spring-boot:run
```

Eureka dashboard: http://localhost:8761 — confirm all five services (including
`api-gateway` and `auth-service`) show as `UP` before testing.

## 4. Using JWT auth (via the gateway, port 8080)

```bash
# Register a recruiter
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"recruiter1","password":"pass123","role":"RECRUITER"}'

# Log in to get a token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"recruiter1","password":"pass123"}'
# => {"token":"eyJ...", "username":"recruiter1", "role":"RECRUITER"}

# Use the token on a protected endpoint
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
  -d '{"companyName":"Acme","role":"Backend Engineer","location":"Bengaluru","status":"OPEN"}'
```

Do the same with `"role":"CANDIDATE"` to get a token that can submit
applications (`POST /api/applications`) but not create jobs or touch
recruitment records — that's the role-based authorization rubric point
demonstrated end-to-end. See the Postman collection for the full set of
requests with token handling automated.

## Security summary (maps to the rubric)

- **JWT Authentication:** `auth-service` hashes passwords with BCrypt and
  issues HS256 JWTs (24h expiry) carrying the user's role. The gateway
  rejects any non-`/api/auth/**`, non-`/actuator/**` request without a valid
  token (401) *before* it is routed anywhere. Each business service *also*
  independently validates the token and enforces its own role rules — a
  service never blindly trusts the gateway.
- **API Gateway Configuration:** routes for all four downstream services,
  a global JWT filter, and a CORS policy are all configured in
  `api-gateway/src/main/resources/application.yml` and
  `filter/JwtAuthenticationFilter.java`.
