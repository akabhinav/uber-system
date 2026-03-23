# RideX - Uber-like Ride-Hailing Platform

Production-grade ride-hailing platform built with Java 21 + Spring Boot 3.2 microservices and React UI.

## Architecture

| Service | Port | Framework | Database |
|---------|------|-----------|----------|
| trip-service | 3001 | Spring MVC + Statemachine | Postgres + Cassandra |
| location-service | 3002 | Spring WebFlux (reactive) | Redis + Cassandra |
| matching-service | 3003 | Spring MVC + Virtual Threads | Redis + Cassandra |
| auth-service | 3004 | Spring MVC + Security | Postgres + Redis |
| payment-service | 3005 | Spring MVC + Virtual Threads | Postgres + Kafka |
| pricing-service | 3006 | Spring MVC + Virtual Threads | Redis |
| ridex-ui | 3000 | React 18 + Leaflet | - |
| nginx gateway | 8080/8081 | REST / WebSocket proxy | - |

## Infrastructure

- **PostgreSQL 15** - trips, users, payments (ACID)
- **Redis 7** - geospatial, OTP, surge pricing, caching
- **Cassandra 4.1** - driver locations (250K writes/sec), trip history
- **Kafka 3.7** - event-driven async communication
- **Nginx** - API gateway + WebSocket proxy

## Quick Start

```bash
# Start everything
make up

# Check service health
make health

# View logs
make logs

# View specific service logs
make logs s=trip-service

# Initialize Kafka topics (first time)
make topics

# Stop everything
make down
```

## Service Ports

| Component | URL |
|-----------|-----|
| React UI | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| WebSocket Gateway | ws://localhost:8081 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |
| Cassandra | localhost:9042 |
| Kafka | localhost:9092 |

## API Endpoints

### Auth
- `POST /api/auth/otp/send` - Send OTP
- `POST /api/auth/otp/verify` - Verify OTP & get JWT
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Current user

### Trips
- `POST /api/trips` - Request a trip
- `GET /api/trips/{tripId}` - Get trip details
- `GET /api/trips/rider/{riderId}` - Get rider's trips
- `PATCH /api/trips/{tripId}/status` - Update trip status
- `POST /api/trips/{tripId}/cancel` - Cancel trip

### Pricing
- `GET /api/pricing/estimate?distanceKm=5&durationMinutes=15` - Price estimate
- `GET /api/pricing/surge?geohashCell=default` - Surge multiplier

### WebSocket
- `ws://localhost:8081/ws/driver` - Driver location push
- `ws://localhost:8081/ws/rider?driverId={id}` - Rider tracking

## Kafka Topics

| Topic | Partitions | Key |
|-------|-----------|-----|
| ridex.trip.events | 16 | trip_id |
| ridex.location.updates | 32 | driver_id |
| ridex.match.requests | 16 | rider_id |
| ridex.match.results | 16 | driver_id |
| ridex.payment.events | 8 | trip_id |
| ridex.notification.fanout | 16 | user_id |

## Development

### Seed Data
Default test users (phone-based login):
- Riders: `+919876543210`, `+919876543211`, `+919876543212`
- Drivers: `+919876543220`, `+919876543221`, `+919876543222`

### React UI Development
```bash
cd ridex-ui
npm install
npm start  # runs on localhost:3000, proxies API to localhost:8080
```

## Tech Stack
- **Java 21** with Virtual Threads (Project Loom)
- **Spring Boot 3.2** - Web, WebFlux, Security, Data, Kafka
- **React 18** - Hooks, Router, Leaflet maps
- **Docker Compose** - local orchestration
