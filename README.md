# Telecom Subscription Service

![Java 17](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=openjdk)
![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=spring)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-green?style=flat-square&logo=springsecurity)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![Gradle](https://img.shields.io/badge/Gradle-8-blue?style=flat-square&logo=gradle)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

JWT-secured telecom subscription management service — handles customer registration, plan management, and subscription lifecycle with role-based access control.

---

## Architecture

```
REST Client
    │
    ▼
┌─────────────────────────────────────────────┐
│  Spring Security Filter Chain               │
│  JwtAuthenticationFilter                   │
│  (validates Bearer token on every request) │
└─────────────────────────────────────────────┘
    │
    ▼
AuthController          PlanController          SubscriptionController
(POST /auth/*)          (GET|POST|PUT|DELETE    (POST|PUT|DELETE|GET
                         /api/plans)             /api/subscriptions)
    │                       │                         │
    ▼                       ▼                         ▼
AuthService             PlanService             SubscriptionService
    │                       │                         │
    ▼                       ▼                         ▼
CustomerRepository      PlanRepository          SubscriptionRepository
    │                       │                         │
    └───────────────────────┴─────────────────────────┘
                            │
                         MySQL 8
                      (telecomdb)
```

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Security | Spring Security + JWT | — |
| Database | MySQL | 8 |
| Build | Gradle | 8 |
| API Docs | SpringDoc OpenAPI | — |
| Test DB | H2 (in-memory) | — |

---

## Prerequisites

| Software | Version | Notes |
|----------|---------|-------|
| Java | 17+ | `java -version` |
| Gradle | 8+ | or use `./gradlew` wrapper |
| Docker | 20.10+ | for Docker Compose option |
| Docker Compose | 2.x+ | `docker compose version` |
| MySQL | 8+ | only needed for local option |

---

## Quick Start

### Option A: Docker Compose (Recommended)

Starts MySQL and the app in one command.

```bash
git clone https://github.com/mohitshaky/telecom-subscription-service.git
cd telecom-subscription-service

# Copy and configure environment variables
cp .env.example .env
# Edit .env and set DB_PASSWORD and JWT_SECRET before starting

docker-compose up --build
```

App starts on **http://localhost:8082** after containers are healthy.

> MySQL is available at `localhost:3306`.

> **Security note**: Always set a strong `JWT_SECRET` (minimum 32 characters) before running in any non-local environment.

---

### Option B: Local (Manual)

**1. Start MySQL 8** (or point to an existing instance).

**2. Export environment variables:**

```bash
export DB_URL="jdbc:mysql://localhost:3306/telecomdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET=change-this-in-production-minimum-32-characters-long
export JWT_EXPIRATION_MS=86400000
export SERVER_PORT=8082
```

**3. Build and run:**

```bash
./gradlew bootRun
```

Or build a JAR and run it:

```bash
./gradlew build
java -jar build/libs/telecom-subscription-service-*.jar
```

> The database and tables are created automatically on first startup (`createDatabaseIfNotExist=true`, Hibernate DDL auto).

---

## Environment Variables

Copy `.env.example` to `.env` and fill in the values.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/telecomdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | MySQL JDBC URL |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | `root` | Database password |
| `JWT_SECRET` | `change-this-in-production-minimum-32-characters-long` | JWT signing secret — **must change in production** |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiry in milliseconds (default: 24 hours) |
| `SERVER_PORT` | `8082` | Application port |

---

## API Endpoints

### Custom Headers (all endpoints)

| Header | Description | Example |
|--------|-------------|---------|
| `X-Transaction-Id` | Unique transaction identifier | `txn-001` |
| `X-Correlation-Id` | For distributed tracing | `corr-001` |
| `X-Source-Channel` | Originating channel | `WEB`, `MOBILE`, `API` |
| `X-Tenant-Id` | Tenant identifier | `tenant-1` |

---

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/auth/register` | Register a new customer | Public |
| `POST` | `/auth/login` | Login — returns JWT token | Public |

#### Register

```bash
curl -X POST http://localhost:8082/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass@123",
    "phone": "+1-555-0100"
  }'
```

#### Login

```bash
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass@123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

Use the token in subsequent requests:
```bash
-H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Plans

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/plans` | List all active plans (optional `?type=PREPAID`) | Customer / Admin |
| `GET` | `/api/plans/{id}` | Get plan by ID | Customer / Admin |
| `POST` | `/api/plans` | Create a new plan | **Admin only** |
| `PUT` | `/api/plans/{id}` | Update plan details | **Admin only** |
| `DELETE` | `/api/plans/{id}` | Deactivate plan (soft delete) | **Admin only** |

#### Create Plan (Admin)

```bash
curl -X POST http://localhost:8082/api/plans \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Premium Unlimited",
    "type": "POSTPAID",
    "price": 49.99,
    "dataLimitGB": 100,
    "validityDays": 30,
    "description": "Unlimited calls + 100GB data"
  }'
```

---

### Subscriptions

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/subscriptions` | Subscribe to a plan | Customer |
| `PUT` | `/api/subscriptions/{id}/upgrade` | Upgrade to a different plan | Customer |
| `DELETE` | `/api/subscriptions/{id}` | Cancel subscription | Customer |
| `GET` | `/api/subscriptions/my` | List my active subscriptions | Customer |

#### Subscribe to a Plan

```bash
curl -X POST http://localhost:8082/api/subscriptions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": 1
  }'
```

---

## Swagger / API Docs

| Resource | URL |
|----------|-----|
| Swagger UI | http://localhost:8082/swagger-ui.html |
| OpenAPI JSON | http://localhost:8082/api-docs |

> Use the **Authorize** button in Swagger UI and enter `Bearer {your_jwt_token}` to test secured endpoints directly.

---

## Health & Monitoring

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Application health status |
| `GET /actuator/info` | Application info |
| `GET /actuator/metrics` | Application metrics |

```bash
curl http://localhost:8082/actuator/health
```

---

## Running Tests

Tests use **H2 in-memory database** — no MySQL required.

```bash
./gradlew test
```

Test reports are generated at `build/reports/tests/test/index.html`.

---

## Project Structure

```
src/main/java/com/mohit/telecom/subscription/
├── config/       SecurityConfig.java
│                 OpenApiConfig.java
├── controller/   AuthController.java
│                 PlanController.java
│                 SubscriptionController.java
├── dto/          RegisterRequest.java
│                 LoginRequest.java
│                 PlanRequest.java
│                 SubscriptionRequest.java
├── entity/       Customer.java
│                 Plan.java
│                 Subscription.java
├── exception/    TelecomException.java
│                 GlobalExceptionHandler.java
├── repository/   CustomerRepository.java
│                 PlanRepository.java
│                 SubscriptionRepository.java
├── security/     JwtTokenProvider.java
│                 JwtAuthenticationFilter.java
│                 CustomUserDetailsService.java
└── service/      AuthService.java
                  PlanService.java
                  SubscriptionService.java
```

---

## Author

Built by **Mohit** — Senior Java Backend Developer | [Portfolio](https://mohitshaky.github.io) | [GitHub](https://github.com/mohitshaky)
