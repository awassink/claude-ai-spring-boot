# Claude AI Spring Boot — Cab Driver Management API

**Version:** 1.0.1 | **Java:** 21 | **Spring Boot:** 3.4.5

REST API for managing cab drivers, secured with JWT/OAuth2 via Keycloak, backed by PostgreSQL.

You can find the detailed explanation and description of this template in the post [Claude Code Template for Spring Boot](https://piotrminkowski.com/2026/03/24/claude-code-template-for-spring-boot/).

---

## Features

- Full CRUD REST API for `Driver` entity
- JWT/OAuth2 authentication via Keycloak (OAuth2 Resource Server)
- PostgreSQL with Flyway migrations
- Docker Compose for local development
- Kubernetes deployment via Skaffold

## Tech Stack

| Component      | Technology                 |
|----------------|----------------------------|
| Framework      | Spring Boot 3.4.5          |
| Language       | Java 21                    |
| Security       | Spring Security 6, OAuth2  |
| Identity       | Keycloak 26.1              |
| Database       | PostgreSQL 16              |
| Migrations     | Flyway 10                  |
| Build          | Maven 3.9                  |
| Containers     | Docker, Kubernetes         |
| Deployment     | Skaffold v4                |

## Driver Entity

| Field             | Type      | Constraints        |
|-------------------|-----------|--------------------|
| id                | UUID      | PK, auto-generated |
| firstName         | String    | required, max 100  |
| lastName          | String    | required, max 100  |
| licenseNumber     | String    | required, unique   |
| phoneNumber       | String    | optional, max 20   |
| email             | String    | required, unique   |
| licenseExpiryDate | LocalDate | required           |
| active            | Boolean   | required           |
| createdAt         | Instant   | auto (audit)       |
| updatedAt         | Instant   | auto (audit)       |

## REST Endpoints

All endpoints (except `/actuator/health`) require a valid Bearer JWT token.

| Method | Path              | Description      | Status      |
|--------|-------------------|------------------|-------------|
| GET    | /api/drivers      | List all drivers | 200         |
| GET    | /api/drivers/{id} | Get by ID        | 200/404     |
| POST   | /api/drivers      | Create driver    | 201/400/409 |
| PUT    | /api/drivers/{id} | Update driver    | 200/400/404 |
| DELETE | /api/drivers/{id} | Delete driver    | 204/404     |
| GET    | /actuator/health  | Health check     | 200         |

## Running Locally

### Prerequisites
- Docker + Docker Compose
- Java 21

### Start with Docker Compose

```bash
docker compose up
```

This starts:
- **PostgreSQL** on `localhost:5432`
- **Keycloak** on `localhost:8180` (admin: `admin` / `admin`)
- **App** on `localhost:8080`

### Get an Access Token

```bash
curl -s -X POST http://localhost:8180/realms/drivers/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=drivers-client&client_secret=drivers-secret&grant_type=client_credentials" \
  | jq -r '.access_token'
```

### Call the API

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/drivers/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=drivers-client&client_secret=drivers-secret&grant_type=client_credentials" \
  | jq -r '.access_token')

# List drivers
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/drivers

# Create a driver
curl -X POST http://localhost:8080/api/drivers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "licenseNumber": "LIC-2024-001",
    "phoneNumber": "+31612345678",
    "email": "john.doe@example.com",
    "licenseExpiryDate": "2027-01-01",
    "active": true
  }'
```

## Running Tests

```bash
# Unit + web layer tests (no Docker required)
mvn test -Dtest="DriverServiceTest,DriverControllerTest"

# All tests including integration (requires Docker)
mvn verify
```

## Kubernetes Deployment (Skaffold)

```bash
# Prerequisites: minikube or any Kubernetes cluster + Skaffold

# Continuous dev loop
skaffold dev

# One-shot deploy
skaffold run
```

## Project Structure

```
src/main/java/nl/awassink/
├── ClaudeAiSpringBootApplication.java
├── domain/
│   ├── model/Driver.java
│   └── repository/DriverRepository.java
├── application/
│   ├── dto/DriverRequest.java
│   ├── dto/DriverResponse.java
│   ├── mapper/DriverMapper.java
│   └── service/DriverService.java
├── infrastructure/
│   ├── config/GlobalExceptionHandler.java
│   ├── config/JpaConfig.java
│   └── security/SecurityConfig.java
└── presentation/
    └── rest/DriverController.java
```
