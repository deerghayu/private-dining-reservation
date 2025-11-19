-- Minimal schema for Private Dining Reservation System

CREATE TABLE restaurants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    timezone VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    min_capacity INTEGER NOT NULL CHECK (min_capacity >= 1),
    max_capacity INTEGER NOT NULL CHECK (max_capacity >= min_capacity),
    minimum_spend_amount DECIMAL(10, 2) NOT NULL,
    minimum_spend_currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    reservation_date DATE NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    party_size INTEGER NOT NULL CHECK (party_size >= 1),
    diner_name VARCHAR(255) NOT NULL,
    diner_email VARCHAR(255) NOT NULL,
    diner_phone VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    special_requests VARCHAR(500),
    cancellation_reason VARCHAR(500),
    cancelled_by VARCHAR(255),
    confirmed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Core constraint: Prevent double-booking while allowing rebooking of cancelled slots
CREATE UNIQUE INDEX uk_room_date_slot_active
ON reservations (room_id, reservation_date, time_slot)
WHERE status IN ('PENDING', 'CONFIRMED');
