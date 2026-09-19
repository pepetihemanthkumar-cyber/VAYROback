# VAYRO — Microservices Backend

Scalable, event-driven microservices backend platform for the VAYRO self-drive vehicle rental service.

## Architecture

```text
               +-----------------------+
               |      API Gateway      | (Port 8080)
               |  (Spring Cloud / JWT) |
               +-----------+-----------+
                           |
       +-------------------+-------------------+
       |                   |                   |
+------+------+     +------+------+     +------+------+
| USER-SERVICE|     | VEHICLE-SRV |     | BOOKING-SRV |
| (Port 8081) |     | (Port 8082) |     | (Port 8083) |
+------+------+     +------+------+     +------+------+
       |                   |                   |
       +-------------------+-------------------+
                           |
               +-----------+-----------+
               |  NOTIFICATION-SERVICE | (Port 8084)
               | (PDF Invoice & Mail)  |
               +-----------+-----------+
                           |
               +-----------+-----------+
               |  Supabase PostgreSQL  | (Port 5432 / Pooler)
               +-----------------------+
```

## Microservices Modules
1. **`eureka-server`** (Port 8761): Spring Cloud Netflix Eureka Service Discovery.
2. **`api-gateway`** (Port 8080): Spring Cloud Gateway with reactive CORS, route dispatching, and JWT security filtering.
3. **`user-service`** (Port 8081): User registration, BCrypt authentication, Google OAuth verification, and JWT issuance.
4. **`vehicle-service`** (Port 8082): Vehicle catalogue, multi-criteria filtering, pricing, and 207-vehicle idempotent seed.
5. **`booking-service`** (Port 8083): Reservation workflow, overlap protection, dynamic pricing, and invoice triggers.
6. **`notification-service`** (Port 8084): PDF invoice generator (OpenPDF) and email dispatcher.

## Technology Stack
- **Java**: Version 21 (LTS)
- **Framework**: Spring Boot 3.4.3 & Spring Cloud 2024.0.0
- **Database**: Supabase PostgreSQL 17 (Session Pooler mode)
- **ORM / Persistence**: Spring Data JPA / Hibernate PostgreSQLDialect
- **Connection Pool**: HikariCP (optimized 3-connection pools)
- **Security**: Spring Security + Stateless HMAC-SHA256 JWT
- **Build Tool**: Apache Maven (Multi-module reactor)

## Setup & Running Locally

### 1. Build and Test
```bash
mvn clean test
mvn clean package -DskipTests
```

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Provide the database connection credentials and JWT secret.

### 3. Run Microservices
Launch services in order:
```bash
# Terminal 1 - Service Discovery
mvn spring-boot:run -pl eureka-server

# Terminal 2 - User Service
mvn spring-boot:run -pl user-service

# Terminal 3 - Vehicle Service
mvn spring-boot:run -pl vehicle-service

# Terminal 4 - Booking Service
mvn spring-boot:run -pl booking-service

# Terminal 5 - Notification Service
mvn spring-boot:run -pl notification-service

# Terminal 6 - API Gateway
mvn spring-boot:run -pl api-gateway
```
