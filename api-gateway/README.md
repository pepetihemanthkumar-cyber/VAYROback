# VAYRO API Gateway

## 1. Overview & Purpose
The **VAYRO API Gateway** is the single, centralized edge entry point (`http://localhost:8080`) for all client traffic across the VAYRO platform. Built on **Spring Cloud Gateway (Spring Boot 3.4.3 / Java 21 / Spring Cloud 2024.0.0)** and **Spring WebFlux Reactive Security**, it orchestrates dynamic request routing, centralized JWT token validation, CORS policy enforcement, resilience timeouts, and observability.

---

## 2. Port & Service Discovery Configuration
- **Application Name**: `API-GATEWAY`
- **Server Port**: `8080`
- **Eureka Server Discovery**: Discovers all downstream services dynamically via `http://localhost:8761/eureka/`.
- **Instance Registration**: Registers with Eureka using `instance-id: ${spring.application.name}:${server.port}`.

---

## 3. Dynamic Routing Matrix
All routes preserve downstream API paths without destructive rewrite or strip-prefix filters:

| Route ID | Route Pattern | Target Discovery URI | Preserved Path Format | Downstream Service |
| :--- | :--- | :--- | :--- | :--- |
| `user-auth-route` | `/api/auth/**` | `lb://USER-SERVICE` | `/api/auth/register`, `/api/auth/login` | `USER-SERVICE` (:8081) |
| `user-service-route` | `/api/users/**` | `lb://USER-SERVICE` | `/api/users/me`, `/api/users` | `USER-SERVICE` (:8081) |
| `vehicle-service-route`| `/api/vehicles/**` | `lb://VEHICLE-SERVICE` | `/api/vehicles`, `/api/vehicles/{id}` | `VEHICLE-SERVICE` (:8082) |
| `booking-service-route`| `/api/bookings/**` | `lb://BOOKING-SERVICE` | `/api/bookings`, `/api/bookings/my` | `BOOKING-SERVICE` (:8083) |
| `gateway-actuator` | `/actuator/**` | Local Gateway | `/actuator/health`, `/actuator/info` | `API-GATEWAY` (:8080) |

---

## 4. Security Architecture & JWT Validation
VAYRO implements **Defense in Depth**:
1. **Gateway Boundary**:
   - Validates HMAC-SHA256 signature and expiration using JJWT 0.12.6 against `${JWT_SECRET}`.
   - Extracts standard claims: `userId`, `email`, `role`, `name`.
   - Populates reactive `SecurityContext` with `ROLE_<role>` authorities.
   - Preserves original `Authorization: Bearer <token>` header downstream.
2. **Public Endpoints**:
   - `POST /api/auth/register`
   - `POST /api/auth/login`
   - `GET /api/vehicles/**` (public vehicle catalogue and details)
   - `GET /api/bookings/availability/**` (public vehicle date availability check)
   - `GET /actuator/health`
3. **Protected Endpoints (JWT Required)**:
   - `GET /api/users/me`, `GET /api/users/**`
   - `POST /api/bookings`, `GET /api/bookings/my`, `GET /api/bookings/{id}`, `PATCH /api/bookings/**`
   - Vehicle mutations (`POST`, `PUT`, `PATCH`, `DELETE` on `/api/vehicles/**`)
4. **Downstream Authoritative RBAC**:
   - Admin-only operations (e.g. `PATCH /api/bookings/{id}/confirm`, vehicle CRUD) are authoritatively authorized at the microservice level. If a standard `USER` reaches an admin endpoint, downstream service rejects with `403 FORBIDDEN`.

---

## 5. Standardized Error Handling
Gateway errors return structured JSON payloads without leaking stack traces or internal hostnames:

- **401 Unauthorized**:
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Authentication is required.",
  "path": "/api/bookings"
}
```

- **403 Forbidden**:
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "Access denied.",
  "path": "/api/bookings/1/confirm"
}
```

- **503 Service Unavailable (Downstream Offline)**:
```json
{
  "status": 503,
  "error": "SERVICE_UNAVAILABLE",
  "message": "The requested VAYRO service is currently unavailable.",
  "path": "/api/bookings"
}
```

---

## 6. CORS Configuration
- **Allowed Origins**: `http://localhost:5173` (customizable via `${CORS_ALLOWED_ORIGINS}`).
- **Allowed Methods**: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- **Allowed Headers**: `*` (including `Authorization`, `Content-Type`).
- **Exposed Headers**: `Authorization`.
- **Credentials**: `true`.
- **Max Age**: 3600 seconds.

---

## 7. HTTP Client Resilience & Timeouts
- **Connect Timeout**: `3000 ms` (`${GATEWAY_CONNECT_TIMEOUT:3000}`)
- **Response Timeout**: `5000 ms` (`${GATEWAY_RESPONSE_TIMEOUT:5s}`)
- **No Unsafe Retries**: Non-idempotent operations like `POST /api/bookings` are never retried automatically to prevent duplicate bookings.

---

## 8. Observability & Logging
- **Health Check**: `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`.
- **Audit Logging**: `LoggingGlobalFilter` logs method, path, and HTTP status for all traffic without logging Authorization headers, passwords, or sensitive payloads.

---

## 9. Local Startup & Testing

### Running API Gateway locally:
```bash
cd backend/vayro-backend/api-gateway
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```

### Running Gateway Unit & Integration Tests:
```bash
mvn test -pl api-gateway
```
