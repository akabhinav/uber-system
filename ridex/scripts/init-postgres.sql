-- RideX PostgreSQL Schema (idempotent)

DO $$ BEGIN
    CREATE TYPE trip_status AS ENUM (
        'REQUESTED','MATCHED','DRIVER_EN_ROUTE',
        'PICKUP','IN_PROGRESS','COMPLETED','CANCELLED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM (
        'PENDING','HELD','CAPTURED','REFUNDED','FAILED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone      VARCHAR(20) UNIQUE NOT NULL,
    name       VARCHAR(120),
    created_at TIMESTAMPTZ DEFAULT now(),
    is_active  BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS drivers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users(id),
    license_no   VARCHAR(40) UNIQUE,
    rating       NUMERIC(3,2) DEFAULT 5.00,
    is_available BOOLEAN DEFAULT false,
    current_lat  DOUBLE PRECISION,
    current_lng  DOUBLE PRECISION,
    updated_at   TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS vehicles (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID REFERENCES drivers(id),
    model     VARCHAR(80),
    plate     VARCHAR(20) UNIQUE,
    capacity  INT DEFAULT 4
);

CREATE TABLE IF NOT EXISTS trips (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rider_id         UUID REFERENCES users(id),
    driver_id        UUID REFERENCES drivers(id),
    status           trip_status NOT NULL DEFAULT 'REQUESTED',
    pickup_lat       DOUBLE PRECISION,
    pickup_lng       DOUBLE PRECISION,
    dropoff_lat      DOUBLE PRECISION,
    dropoff_lng      DOUBLE PRECISION,
    fare_cents       INT,
    surge_multiplier NUMERIC(4,2) DEFAULT 1.0,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS payments (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id                  UUID REFERENCES trips(id),
    amount_cents             INT NOT NULL,
    currency                 VARCHAR(3) DEFAULT 'USD',
    status                   payment_status DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(120),
    idempotency_key          VARCHAR(120) UNIQUE NOT NULL,
    created_at               TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS wallets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id     UUID REFERENCES drivers(id) UNIQUE,
    balance_cents INT DEFAULT 0,
    updated_at    TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_trips_rider   ON trips(rider_id);
CREATE INDEX IF NOT EXISTS idx_trips_driver  ON trips(driver_id);
CREATE INDEX IF NOT EXISTS idx_trips_status  ON trips(status);
CREATE INDEX IF NOT EXISTS idx_trips_created ON trips(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_drivers_avail ON drivers(is_available);
