CREATE KEYSPACE IF NOT EXISTS ridex WITH replication = {
    'class': 'SimpleStrategy', 'replication_factor': 1
};

USE ridex;

CREATE TABLE IF NOT EXISTS driver_locations (
    driver_id   uuid,
    recorded_at timestamp,
    lat         double,
    lng         double,
    speed       float,
    heading     float,
    PRIMARY KEY (driver_id, recorded_at)
) WITH CLUSTERING ORDER BY (recorded_at DESC)
  AND default_time_to_live = 30;

CREATE TABLE IF NOT EXISTS trip_history_by_rider (
    rider_id    uuid,
    created_at  timestamp,
    trip_id     uuid,
    status      text,
    fare_cents  int,
    surge       decimal,
    pickup_lat  double,
    pickup_lng  double,
    dropoff_lat double,
    dropoff_lng double,
    PRIMARY KEY (rider_id, created_at, trip_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

CREATE TABLE IF NOT EXISTS trip_history_by_driver (
    driver_id  uuid,
    created_at timestamp,
    trip_id    uuid,
    status     text,
    fare_cents int,
    PRIMARY KEY (driver_id, created_at, trip_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

CREATE TABLE IF NOT EXISTS trip_events (
    trip_id    uuid,
    event_id   timeuuid,
    event_type text,
    payload    text,
    PRIMARY KEY (trip_id, event_id)
) WITH CLUSTERING ORDER BY (event_id ASC);
