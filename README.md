# Private Dining Reservation System

OpenTable take-home assignment implemented in Java/Spring Boot.


## Stack
- Java 17, Spring Boot 3.5.8
- Spring Validation
- Spring Cache
- Kafka


# Features
- **Three-layer double booking prevention**: availability check, unique index, optimistic locking.
- **Availability slot**: returns slots for any date window.
- **Event bus**: `create.reservation` / `cancel.reservation` events fan out to listeners.
- **Composable services**: restaurants, rooms, reservations separated but collocated for now.