# VAYRO Booking Microservice (`booking-service`)

## 1. Overview & Architecture
The **VAYRO Booking Microservice** is a core component of the VAYRO platform built with Spring Boot 3.4.3 and Java 21. It manages the complete vehicle reservation lifecycle, strictly enforces zero double-booking overlap invariant rules, performs deterministic server-side pricing and add-ons calculations using `BigDecimal`, and communicates asynchronously with Eureka Server and Vehicle Service (`VEHICLE-SERVICE`).

### Technology Stack & Ports
- **Microservice Port**: `8083`
- **Eureka Discovery**: `http://localhost:8761/eureka/` (registers as `BOOKING-SERVICE`)
- **Actuator Health**: `http://localhost:8083/actuator/health`
- **Database**: MySQL 8+ (`vayro_bookings`)
- **HTTP Client**: `RestClient` with `JdkClientHttpRequestFactory` (configured with 3s connect timeout and 5s read timeout)
- **Security**: Stateless JWT HMAC-SHA filter chain with fine-grained `@PreAuthorize("hasRole('ADMIN')")` role-based access control and custom authentication entry point / access denied handlers.

---

## 2. Booking State Machine & Lifecycle Rules

The booking lifecycle is strictly governed by a unidirectional deterministic finite state machine. Arbitrary status modifications or bypasses via generic endpoints are forbidden.

```
       [ PENDING ]
         ├── (Admin: Confirm) ───────────► [ CONFIRMED ]
         │                                     ├── (Admin: Activate) ──► [ ACTIVE ]
         │                                     │                           │
         │                                     │                           ├── (Admin: Return) ──► [ RETURNED ]
         │                                     │                           │                           │
         │                                     │                           │                           └── (Admin: Complete) ──► [ COMPLETED ]
         │                                     │                           ▼
         └── (Customer/Admin: Cancel) ─► [ CANCELLED ] ◄── (Customer/Admin: Cancel)
```

### Transition Verification Matrix
| Initial State | Target State | Permitted Actor | Vehicle Sync Action | Result |
|---|---|---|---|---|
| `PENDING` | `CONFIRMED` | `ADMIN` | None (Vehicle remains available for other time slots) | Allowed |
| `PENDING` | `CANCELLED` | Owner / `ADMIN` | None | Allowed |
| `CONFIRMED` | `ACTIVE` | `ADMIN` | Vehicle updated to `RENTED` in Vehicle Service | Allowed |
| `CONFIRMED` | `CANCELLED` | Owner / `ADMIN` | None | Allowed |
| `ACTIVE` | `RETURNED` | `ADMIN` | Vehicle updated to `AVAILABLE` in Vehicle Service | Allowed |
| `RETURNED` | `COMPLETED` | `ADMIN` | None (Vehicle remains `AVAILABLE`) | Allowed |
| `PENDING` | `ACTIVE` / `RETURNED` / `COMPLETED` | Any | — | **Rejected** (HTTP 400 `INVALID_BOOKING_STATE`) |
| `CONFIRMED` | `RETURNED` / `COMPLETED` | Any | — | **Rejected** (HTTP 400 `INVALID_BOOKING_STATE`) |
| `ACTIVE` | `CONFIRMED` / `CANCELLED` | Any | — | **Rejected** (HTTP 400 `INVALID_BOOKING_STATE`) |
| `RETURNED` | `ACTIVE` / `CANCELLED` | Any | — | **Rejected** (HTTP 400 `INVALID_BOOKING_STATE`) |
| `COMPLETED` | Any | Any | — | **Rejected** (HTTP 400 `INVALID_BOOKING_STATE`) |
| `CANCELLED` | Any | Any | — | **Rejected** (HTTP 400 `INVALID_BOOKING_STATE`) |

---

## 3. Canonical Overlap Prevention & Availability Rule

The booking engine enforces a single canonical overlap rule across creation, availability checking, and repository queries:

$$\text{Overlap Condition} \iff (\text{existing}.\text{pickupDateTime} < \text{requested}.\text{returnDateTime}) \land (\text{existing}.\text{returnDateTime} > \text{requested}.\text{pickupDateTime})$$

### Blocking vs Non-Blocking Statuses
- **Blocking Statuses**: `PENDING`, `CONFIRMED`, `ACTIVE` (locks the slot against any other reservation).
- **Non-Blocking Statuses**: `CANCELLED`, `RETURNED`, `COMPLETED` (releases the slot for new reservations).

### Boundary Condition Invariants
- **Adjacent Bookings (Exact Boundary)**: If Booking A is `10:00 to 12:00` and Booking B is `12:00 to 14:00`, it is **ALLOWED** (0 minute overlap).
- **Partial Overlap (1-Minute)**: If Booking A is `10:00 to 12:00` and Booking B is `09:59 to 10:01`, it is **REJECTED** (HTTP 409 `BOOKING_CONFLICT`).
- **Contained / Surrounding Bookings**: Any overlap where one booking is fully inside or enclosing another is **REJECTED**.

---

## 4. Concurrency Hardening & Locking Strategy

### Current Strategy: Striped Bounded Locks
To protect simultaneous booking requests within the service instance:
1. A bounded striped lock table (`Object[]` with 128 stripes) hashes `vehicleId.hashCode()`.
2. Concurrent requests for the same vehicle queue predictably without thread race windows.
3. Completely avoids unbounded heap memory growth (no memory leak compared to unbound map keys).
4. Synchronized blocks enclose the canonical overlap check, vehicle validation, and JPA save operations inside the `@Transactional` boundary.

### Architectural Note on Distributed Concurrency
> [!IMPORTANT]
> Current protection is JVM-local and is not sufficient as the sole distributed lock for multi-instance production deployment. In a horizontally scaled cluster behind an API Gateway / Load Balancer, distributed coordination (such as Redis/Redisson distributed lock or database row-level pessimistic locking `SELECT ... FOR UPDATE`) must be added.

---

## 5. Pricing Integrity & Rental Duration Formula

All monetary calculations strictly use `java.math.BigDecimal` with `RoundingMode.HALF_UP` (2 decimal places). Frontend pricing parameters are discarded and never trusted.

### Business Calculation Rules:
1. **Rental Duration Rule**:
   $$\text{durationMinutes} = \text{ChronoUnit.MINUTES.between}(\text{pickupDateTime}, \text{returnDateTime})$$
   $$\text{rentalDays} = \max(1, \lceil \text{durationMinutes} / 1440.0 \rceil)$$
2. **Vehicle Daily Rate**: Always fetched from Vehicle Service (`pricePerDay`).
3. **Base Amount**: $\text{baseAmount} = \text{pricePerDay} \times \text{rentalDays}$
4. **Add-Ons Amount**: $\sum (\text{quantity} \times \text{unitPrice})$ where $\text{quantity} \ge 1$, $\text{unitPrice} \ge 0$, and add-on name is mandatory.
5. **Discount Amount**: Validated and capped so $\text{discountAmount} \le \text{baseAmount} + \text{addOnsAmount}$.
6. **Subtotal**: $\text{subtotal} = \text{baseAmount} + \text{addOnsAmount} - \text{discountAmount}$
7. **Tax Amount**: $\text{taxAmount} = \text{subtotal} \times \text{BOOKING\_TAX\_RATE}$ (Default: `0.18` GST).
8. **Total Amount**: $\text{totalAmount} = \text{subtotal} + \text{taxAmount}$ (Guaranteed non-negative).

---

## 6. Vehicle Service Communication & Failure Handling

The Booking Service calls `VEHICLE-SERVICE` via load-balanced `RestClient`:
1. **Vehicle Verification**: Verifies vehicle existence, active status, and current catalog price. If `VEHICLE-SERVICE` is unreachable or returns 404/500, Booking Service fails safely with a structured `VehicleUnavailableException` (HTTP 503 / 400) and does **NOT** create a booking with guessed data.
2. **Vehicle Status Sync**:
   - `CONFIRMED -> ACTIVE` $\to$ sets vehicle status to `RENTED`.
   - `ACTIVE -> RETURNED` $\to$ sets vehicle status to `AVAILABLE`.
   - `RETURNED -> COMPLETED` $\to$ vehicle remains `AVAILABLE`.
3. **Resilience**: `JdkClientHttpRequestFactory` enforces a 3-second connect timeout and 5-second read timeout, preventing thread starvation.

---

## 7. Security & User Ownership Matrix

- **USER Role**:
  - Can create reservations for themselves (userId extracted exclusively from validated JWT claims).
  - Can view their own reservations (`/api/bookings/my`, `/api/bookings/{id}`).
  - Can cancel their own eligible reservations (`PENDING` or `CONFIRMED`).
  - Cannot access or view other customers' bookings (HTTP 403 `FORBIDDEN`).
  - Cannot trigger admin lifecycle actions (`confirm`, `activate`, `return`, `complete`) or list all fleet bookings (HTTP 403 `FORBIDDEN`).
- **ADMIN Role**:
  - Can view and search all bookings with pagination (`GET /api/bookings`).
  - Can manage full lifecycle transitions (`confirm`, `activate`, `return`, `complete`, `cancel`).

---

## 8. REST API Endpoints

| HTTP Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `POST` | `/api/bookings` | `USER`, `ADMIN` | Create a new vehicle reservation |
| `GET` | `/api/bookings/my` | `USER`, `ADMIN` | List authenticated user's bookings (paginated) |
| `GET` | `/api/bookings/{id}` | Owner or `ADMIN` | Get booking details by numeric ID |
| `GET` | `/api/bookings/reference/{ref}` | Owner or `ADMIN` | Get booking details by reference number |
| `GET` | `/api/bookings` | `ADMIN` only | Query/filter all fleet bookings with pagination |
| `PATCH` | `/api/bookings/{id}/cancel` | Owner or `ADMIN` | Cancel a booking (`PENDING`/`CONFIRMED` $\to$ `CANCELLED`) |
| `PATCH` | `/api/bookings/{id}/confirm` | `ADMIN` only | Confirm reservation (`PENDING` $\to$ `CONFIRMED`) |
| `PATCH` | `/api/bookings/{id}/activate` | `ADMIN` only | Activate on vehicle pickup (`CONFIRMED` $\to$ `ACTIVE`) |
| `PATCH` | `/api/bookings/{id}/return` | `ADMIN` only | Return vehicle (`ACTIVE` $\to$ `RETURNED`) |
| `PATCH` | `/api/bookings/{id}/complete` | `ADMIN` only | Complete lifecycle (`RETURNED` $\to$ `COMPLETED`) |
| `GET` | `/api/bookings/availability/{vehicleId}` | Public / All | Check vehicle availability for date range |

---

## 9. Configuration & Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | MySQL hostname | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database schema name | `vayro_bookings` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `EUREKA_SERVER_URL` | Eureka Service URL | `http://localhost:8761/eureka/` |
| `JWT_SECRET` | 256-bit HMAC signing key | Shared dev key |
| `BOOKING_TAX_RATE` | Tax rate percentage | `0.18` |
| `CORS_ALLOWED_ORIGINS` | Allowed origins | `http://localhost:5173` |

---

## 10. Testing & Verification

### Running Booking Service Tests (68 Scenarios)
```bash
cd backend/vayro-backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn test -pl booking-service
```

### Running Complete Backend Regression Test Suite
```bash
cd backend/vayro-backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean test
```
