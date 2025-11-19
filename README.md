# Private Dining Reservation System

A scalable, event-driven reservation system for private dining rooms built with Spring Boot.

![CI](https://github.com/deerghayu/private-dining-reservation/workflows/CI/badge.svg)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/deerghayu/private-dining-reservation.git
cd private-dining-reservation
```

## Quick Start

### Prerequisites

- Java 17+
- Docker Desktop (must be running)
- Maven 3.8+ (or use included `./mvnw`)

### Run Tests

Ensure Docker Desktop is running, then:

```bash
./mvnw test
```

Tests automatically start PostgreSQL via Testcontainers.

### Run Application Locally

**Step 1: Start PostgreSQL**

Ensure Docker Desktop is running, then:

```bash
docker-compose up -d
```

**Step 2: Run Application**

```bash
./mvnw spring-boot:run
```

**Step 3: Access Swagger UI**

Visit: http://localhost:8080/swagger-ui.html

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## API Endpoints

Base URL: `http://localhost:8080/api/v1`

### Restaurants & Rooms

| Method | Endpoint                                                                                  | Description        |
|--------|-------------------------------------------------------------------------------------------|--------------------|
| GET    | `/restaurants?city={city}`                                                                | List restaurants   |
| GET    | `/restaurants/{restaurantId}/rooms`                                                       | List rooms         |
| GET    | `/restaurants/{restaurantId}/rooms/{roomId}/availability?startDate={date}&endDate={date}` | Check availability |

### Reservations

| Method | Endpoint                         | Description                    |
|--------|----------------------------------|--------------------------------|
| POST   | `/reservations`                  | Create reservation             |
| GET    | `/reservations/{id}`             | Get reservation                |
| POST   | `/reservations/{id}/cancel`      | Cancel reservation             |
| GET    | `/diners/{email}/reservations`   | List diner's reservations      |
| GET    | `/restaurants/{id}/reservations` | List restaurant's reservations |

### Example: Create Reservation

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "reservationDate": "2025-12-25",
    "timeSlot": "DINNER",
    "partySize": 4,
    "diner": {
      "name": "Sam Smith",
      "email": "sam.smith@example.com",
      "phone": "+1-308-1234"
    },
    "estimatedSpend": {
      "amount": 600.00,
      "currency": "USD"
    }
  }'
```

## Database Setup

PostgreSQL runs in Docker using the included `docker-compose.yml` file in the project root:

- **Database**: pdrs
- **User**: pdrs
- **Password**: pdrs
- **Port**: 5432

Schema migrations run automatically via Flyway on application startup.

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=ConcurrencyTest

# Generate coverage report
./mvnw test jacoco:report
# View report at: target/site/jacoco/index.html
```

**Key Test:** `ConcurrencyTest` - 20 threads attempt to book the same slot simultaneously - only 1 succeeds, proving the double-booking prevention works.

## Key Features

- **Zero double-bookings:** Partial unique index at database level makes conflicts impossible, even with buggy code
- **Event-driven:** Notifications and audit logs happen async - they don't block the booking response
- **Production-ready caching:** Restaurant/room catalog and availability queries cached for performance
- **Tested under concurrency:** 70%+ coverage including tests where 20 threads simultaneously compete for the same slot
- **Schema versioning:** Flyway migrations handle database changes

## AI Tools Usage

- IntelliJ IDEA's AI features for code completion, logging statements, Swagger annotations, and Javadocs
- Documentation structure and organization (README, architecture docs)
- Test coverage analysis

## Design Documentation

**[SYSTEM-DESIGN-DOCUMENT](system-design/SYSTEM-DESIGN-DOCUMENT.pdf)** - Complete system design, architecture decisions, and
scalability plan