# Private Dining Reservation System

A scalable, event-driven reservation system for private dining rooms built with Spring Boot. This is a take-home assignment for a Senior Software Engineer position at OpenTable.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Design Decisions](#design-decisions)

---

## Features

### Core Functionality
- **Restaurant & Room Management**: Browse restaurants and private dining rooms
- **Reservation Creation**: Book rooms with validation (capacity, minimum spend)
- **Availability Checking**: Real-time availability for date ranges
- **Reservation Cancellation**: Cancel with reason tracking
- **Diner Reservations**: View all or upcoming reservations per diner
- **Staff Reservations**: View all reservations per restaurant

### Technical Features
- **Concurrency Control**: Industry-standard defense against double-booking
  - PostgreSQL partial unique index (database-enforced)
  - Application-level validation (defense in depth)
  - Optimistic locking with @Version fields
- **Event-Driven Architecture**: Spring Events for asynchronous processing
  - Notification events (simulated email sending)
  - Audit logging
  - Analytics tracking
- **Database Migrations**: Flyway for version-controlled schema management
- **Comprehensive Testing**: 70%+ code coverage
  - Unit tests
  - Integration tests
  - Concurrency tests (proves double-booking prevention)

---

## Tech Stack

### Backend
- **Java 17** - Modern Java with records, pattern matching
- **Spring Boot 3.5.8** - Application framework
- **Spring Data JPA** - Database access with Hibernate
- **Spring Events** - In-memory event bus for async processing
- **Spring Validation** - Request validation

### Database
- **PostgreSQL 16** - Primary database (see [SYSTEM-DESIGN-DOCUMENT.md](docs/SYSTEM-DESIGN-DOCUMENT.md) for justification)
- **Flyway** - Database migration tool
- **Testcontainers** - PostgreSQL containers for testing (ensures test/prod parity)

### Tools & Libraries
- **Lombok** - Reduces boilerplate code
- **SpringDoc OpenAPI** - API documentation (Swagger UI)
- **JUnit 5** - Testing framework
- **AssertJ** - Fluent assertions for tests
- **Mockito** - Mocking framework

---

## Architecture

See **[SYSTEM-DESIGN-DOCUMENT.md](docs/SYSTEM-DESIGN-DOCUMENT.md)** for comprehensive system design documentation including:
- Complete system architecture with visual diagrams
- PostgreSQL vs MongoDB decision analysis with trade-offs
- Database schema with ER diagram
- Event-driven architecture implementation
- Concurrency control strategy (multi-layer defense)
- Scalability plan for high-traffic scenarios
- API design and endpoint specifications

### Visual Diagrams

All architecture diagrams are available in the `docs/diagrams/` folder:

- **[Database ER Diagram](docs/diagrams/database-er-diagram.md)** - Complete entity-relationship model with constraints
- **[System Architecture Diagram](docs/diagrams/system-architecture.md)** - Layered architecture and component interactions
- **[Component Diagram](docs/diagrams/component-diagram.md)** - Component responsibilities and communication patterns
- **[Sequence Diagrams](docs/diagrams/sequence-diagrams.md)** - Critical flows including:
  - Reservation creation with event processing
  - Concurrent double-booking prevention
  - Cancellation and rebooking flow

### High-Level Design

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
┌──────▼────────────────────────┐
│  REST API (Controllers)       │
├───────────────────────────────┤
│  Service Layer                │
│  - Business Logic             │
│  - Event Publishing           │
├───────────────────────────────┤
│  Repository Layer (JPA)       │
└──────┬────────────────────────┘
       │
┌──────▼────────┐
│  PostgreSQL   │
└───────────────┘

   Event Listeners (Async)
   ┌────────────────────┐
   │  Notification      │
   │  Audit Log         │
   │  Analytics         │
   └────────────────────┘
```

---

## Getting Started

### Prerequisites
- Java 17+
- Docker
- Maven 3.8+ (or use included `./mvnw`)

### Quick Start: Run Tests

```bash
./mvnw test
```

**Note:** Docker Desktop must be running. Tests use Testcontainers to automatically start PostgreSQL and verify double-booking prevention.

### Run the Application Locally

**Note:** Docker Desktop must be running.

**Step 1: Start PostgreSQL**

```bash
docker-compose up -d
```

This starts PostgreSQL on `localhost:5432` with database `pdrs`, user `pdrs`, password `pdrs`.

**Step 2: Run the Application**

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

**Step 3: Access API Documentation**

Visit: `http://localhost:8080/swagger-ui.html`

**Step 4: Health Check**

```bash
curl http://localhost:8080/actuator/health
```

---

## API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints

#### Restaurants & Rooms

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/restaurants?city={city}` | List restaurants (optional filter by city) |
| GET | `/restaurants/{restaurantId}/rooms` | List rooms for a restaurant |
| GET | `/restaurants/{restaurantId}/rooms/{roomId}/availability?startDate={date}&endDate={date}` | Get availability calendar |

#### Reservations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/reservations` | Create a new reservation |
| GET | `/reservations/{id}` | Get reservation details |
| POST | `/reservations/{id}/cancel` | Cancel a reservation (with request body) |
| GET | `/diners/{email}/reservations?upcomingOnly={bool}&page={int}&size={int}` | List diner's reservations (paginated) |
| GET | `/restaurants/{id}/reservations?page={int}&size={int}` | List restaurant's reservations (paginated, staff) |

### Example: Create Reservation

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "reservationDate": "2025-12-25",
    "timeSlot": "DINNER",
    "partySize": 4,
    "diner": {
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+1-555-1234"
    },
    "estimatedSpend": {
      "amount": 600.00,
      "currency": "USD"
    },
    "specialRequests": "Window seat preferred"
  }'
```

**Response:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "roomName": "Rooftop Terrace",
  "reservationDate": "2025-12-25",
  "timeSlot": "DINNER",
  "partySize": 4,
  "dinerName": "John Doe",
  "dinerEmail": "john@example.com",
  "dinerPhone": "+1-555-1234",
  "status": "CONFIRMED",
  "specialRequests": "Window seat preferred",
  "createdAt": "2025-11-19T10:30:00Z"
}
```

### Time Slots

The system supports four predefined time slots:
- `BREAKFAST` (07:00 - 11:00)
- `LUNCH` (11:00 - 15:00)
- `DINNER` (17:00 - 22:00)
- `LATE_NIGHT` (22:00 - 01:00)

### Reservation Status

- `PENDING`: Awaiting confirmation (not currently used, all reservations are CONFIRMED)
- `CONFIRMED`: Reservation is confirmed
- `CANCELLED`: Reservation has been cancelled

---

## Testing

### Run All Tests

```bash
./mvnw test
```

Tests run against PostgreSQL via Testcontainers (automatic setup).

### Run Specific Test Classes

```bash
# Concurrency tests (critical - proves double-booking prevention)
./mvnw test -Dtest=ConcurrencyTest

# Unit tests
./mvnw test -Dtest=ReservationServiceTest
./mvnw test -Dtest=AvailabilityServiceTest

# Integration tests
./mvnw test -Dtest=ReservationIntegrationTest
```

### Test Coverage

Generate coverage report:

```bash
./mvnw test jacoco:report
```

Report location: `target/site/jacoco/index.html`

**Current Coverage:** 70%+ (primarily service and repository layers)

### Key Tests

#### 1. Concurrency Test - Double-Booking Prevention
Located in `ConcurrencyTest.java`

**Test:** 20 threads simultaneously attempt to book the same room/date/time slot.

**Expected Result:**
- Exactly 1 reservation succeeds
- 19 reservations fail with `RoomAlreadyBookedException`
- Database contains exactly 1 reservation

**How it works:** Tests run against real PostgreSQL (via Testcontainers) with the partial unique index that enforces the constraint at the database level. This proves the system prevents double-booking under high concurrency.

#### 2. Business Logic Validation Tests
Located in `ReservationServiceTest.java`

Tests all validation rules:
- Party size within room capacity
- Minimum spend requirements
- Currency matching
- Room availability
- Inactive room rejection

#### 3. End-to-End Integration Tests
Located in `ReservationIntegrationTest.java`

Tests complete workflows:
- Create → Retrieve → Cancel
- List reservations by diner
- List reservations by restaurant

---

## Design Decisions

### 1. PostgreSQL over MongoDB

**Decision:** Use PostgreSQL instead of the preferred MongoDB

**Rationale:**
- **ACID compliance**: Critical for preventing double-booking
- **Complex queries**: Availability checking requires JOINs
- **Data integrity**: Database-level constraints (unique, foreign keys, check)
- **Mature concurrency**: Row-level locking, transaction isolation
- **Relational data**: Restaurants → Rooms → Reservations is inherently relational

See [SYSTEM-DESIGN-DOCUMENT.md](docs/SYSTEM-DESIGN-DOCUMENT.md) - Section 4.1 for full analysis.

### 2. Spring Events over Kafka

**Decision:** Use Spring's in-memory event bus for async processing

**Rationale:**
- **Requirements**: Event-driven architecture listed as "nice to have"
- **Simplicity**: No external infrastructure needed for MVP
- **Sufficient**: Meets requirements for decoupling components
- **Testability**: Easy to test without Kafka setup
- **Future-proof**: Can migrate to Kafka when needed

**Production Path:** Would use Kafka for:
- Event persistence
- Distributed processing
- Guaranteed delivery
- Event replay

### 3. PostgreSQL Partial Unique Index for Concurrency Control

**Decision:** Use database partial unique index + optimistic locking instead of pessimistic locking

**Rationale:**
- **Industry standard**: Preferred approach at JPMorgan Chase and similar institutions
- **Better performance**: Non-blocking reads, higher throughput
- **Atomic enforcement**: Database guarantees prevent double-booking
- **Allows rebooking**: Cancelled reservations don't block future bookings
- **Simple and reliable**: No distributed locks or complex application logic needed

### 4. UUIDs as Primary Keys

**Decision:** Use UUIDs instead of auto-increment integers

**Rationale:**
- **Distributed-friendly**: Can generate IDs without database round-trip
- **Security**: Non-sequential IDs don't leak information
- **Merge-friendly**: No conflicts when merging data
- **Industry standard**: Common in microservices

### 5. DTOs for API Layer

**Decision:** Separate DTOs (Data Transfer Objects) from entities

**Rationale:**
- **API stability**: Can change database schema without breaking API
- **Security**: Don't expose internal fields (e.g., version, internal IDs)
- **Validation**: API-specific validation separate from entity rules
- **Documentation**: Clear API contracts

---

## Project Structure

```
src/
├── main/
│   ├── java/com/opentable/reservation/
│   │   ├── config/              # Configuration classes
│   │   │   └── AsyncConfiguration.java
│   │   ├── controller/          # REST controllers
│   │   │   ├── ReservationController.java
│   │   │   └── RestaurantController.java
│   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── CreateReservationRequest.java
│   │   │   ├── ReservationResponse.java
│   │   │   └── AvailabilityResponse.java
│   │   ├── event/               # Event classes
│   │   │   ├── ReservationCreatedEvent.java
│   │   │   └── ReservationCancelledEvent.java
│   │   ├── exception/           # Custom exceptions
│   │   │   ├── BusinessException.java
│   │   │   ├── NotFoundException.java
│   │   │   └── RoomAlreadyBookedException.java
│   │   ├── listener/            # Event listeners
│   │   │   ├── NotificationEventListener.java
│   │   │   ├── AuditEventListener.java
│   │   │   └── AnalyticsEventListener.java
│   │   ├── model/               # Domain entities
│   │   │   ├── Restaurant.java
│   │   │   ├── Room.java
│   │   │   ├── Reservation.java
│   │   │   ├── TimeSlot.java
│   │   │   ├── RoomType.java
│   │   │   └── ReservationStatus.java
│   │   ├── repository/          # Data access layer
│   │   │   ├── ReservationRepository.java
│   │   │   ├── RestaurantRepository.java
│   │   │   └── RoomRepository.java
│   │   └── service/             # Business logic
│   │       ├── ReservationService.java
│   │       ├── AvailabilityService.java
│   │       ├── RestaurantService.java
│   │       └── RoomService.java
│   └── resources/
│       ├── application.yml      # Main configuration
│       ├── application-test.yml # Test configuration
│       └── db/migration/        # Flyway migrations
│           └── V1__initial_schema.sql
└── test/
    ├── java/com/opentable/reservation/
    │   ├── concurrency/         # Concurrency tests
    │   │   └── ConcurrencyTest.java
    │   ├── integration/         # Integration tests
    │   │   └── ReservationIntegrationTest.java
    │   ├── service/             # Unit tests
    │   │   ├── ReservationServiceTest.java
    │   │   └── AvailabilityServiceTest.java
    │   └── testutil/            # Test utilities
    │       └── TestDataBuilder.java
    └── resources/
        └── application-test.yml
```

---

## Database Schema

### Tables

**restaurants**
- id (UUID, PK)
- name (VARCHAR)
- city (VARCHAR)
- state (VARCHAR)
- timezone (VARCHAR)
- currency (VARCHAR)
- created_at, updated_at (TIMESTAMP)

**rooms**
- id (UUID, PK)
- restaurant_id (UUID, FK → restaurants)
- name (VARCHAR)
- room_type (VARCHAR): ROOFTOP, HALL, PRIVATE_ROOM, CHEF_TABLE
- min_capacity, max_capacity (INTEGER)
- minimum_spend_amount, minimum_spend_currency
- active (BOOLEAN)
- created_at, updated_at (TIMESTAMP)

**reservations**
- id (UUID, PK)
- restaurant_id (UUID, FK → restaurants)
- room_id (UUID, FK → rooms)
- reservation_date (DATE)
- time_slot (VARCHAR)
- party_size (INTEGER)
- diner_name, diner_email, diner_phone (VARCHAR)
- status (VARCHAR): PENDING, CONFIRMED, CANCELLED
- special_requests (VARCHAR, 500)
- cancelled_by, cancellation_reason, cancelled_at
- created_at, updated_at (TIMESTAMP)
- version (BIGINT) - for optimistic locking
- **Partial Unique Index: (room_id, reservation_date, time_slot) WHERE status IN ('PENDING', 'CONFIRMED')** ← Prevents double-booking while allowing rebooking

### Migrations

Single Flyway migration in `src/main/resources/db/migration/`:

- `V1__initial_schema.sql`: Creates all tables and the partial unique index for double-booking prevention

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_URL | jdbc:postgresql://localhost:5432/pdrs | Database connection URL |
| DB_USERNAME | pdrs | Database username |
| DB_PASSWORD | pdrs | Database password |

### Application Properties

Key configurations in `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20    # Connection pool size
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate        # Use Flyway for schema management
  flyway:
    enabled: true
    baseline-on-migrate: true   # Handle existing databases
```

---

## Future Enhancements

### High Priority
- [ ] Authentication & Authorization (OAuth2/JWT)
- [ ] Rate limiting
- [ ] Reservation modification (PATCH endpoint)
- [ ] Enhanced validation (past dates, business hours)

### Medium Priority
- [ ] Restaurant/Room management APIs (CRUD)
- [ ] Caching layer (Redis) for availability
- [ ] Metrics and monitoring (Micrometer/Prometheus)
- [ ] Search and filtering capabilities
- [ ] Email integration (SendGrid/SES)

### Scalability
- [ ] Migrate to Kafka for events
- [ ] Database read replicas
- [ ] API gateway
- [ ] Circuit breakers (Resilience4j)
- [ ] Distributed tracing (Jaeger/Zipkin)

---

## Known Limitations

1. **No Authentication**: All endpoints are public
2. **Fixed Time Slots**: Cannot customize restaurant-specific hours
3. **No Payment Integration**: No deposits or payment processing
4. **Single Tenant**: No multi-tenancy support
5. **English Only**: No i18n/l10n support

---

## Contributing

This is a take-home assignment and not open for external contributions.

---

## License

This is proprietary code for a job application assessment.

---

## Contact

For questions about this implementation:
- **Author**: [Your Name]
- **Assignment**: OpenTable Senior Software Engineer Take-Home
- **Date**: November 2025

---

## Acknowledgments

- OpenTable for the interesting assignment
- Spring Boot team for excellent documentation
- PostgreSQL community for robust database technology
