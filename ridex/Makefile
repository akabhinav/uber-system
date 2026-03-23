.PHONY: up down logs build migrate schema topics seed osm status health

up:
	docker compose up -d --build

down:
	docker compose down -v

logs:
	docker compose logs -f $(s)

build:
	./mvnw clean package -DskipTests

migrate:
	docker compose exec -T postgres psql -U ridex -d ridex -f /docker-entrypoint-initdb.d/01-init.sql

schema:
	docker compose exec -T cassandra cqlsh -f /etc/cassandra/init-cassandra.cql

topics:
	docker compose exec kafka bash /opt/scripts/init-kafka-topics.sh

seed:
	docker compose exec -T postgres psql -U ridex -d ridex -f /docker-entrypoint-initdb.d/02-seed.sql

osm:
	bash scripts/download-osm.sh

status:
	@docker compose ps
	@echo ""
	@echo "=== Service Ports ==="
	@echo "Gateway REST : http://localhost:8080"
	@echo "Gateway WS   : http://localhost:8081"
	@echo "Trip Service : http://localhost:3001"
	@echo "Location Svc : http://localhost:3002"
	@echo "Matching Svc : http://localhost:3003"
	@echo "Auth Service : http://localhost:3004"
	@echo "Payment Svc  : http://localhost:3005"
	@echo "Pricing Svc  : http://localhost:3006"
	@echo "PostgreSQL   : localhost:5432"
	@echo "Redis        : localhost:6379"
	@echo "Cassandra    : localhost:9042"
	@echo "Kafka        : localhost:9092"

health:
	@echo "=== Health Checks ==="
	@curl -sf http://localhost:3001/actuator/health 2>/dev/null && echo " trip-service: UP" || echo " trip-service: DOWN"
	@curl -sf http://localhost:3002/actuator/health 2>/dev/null && echo " location-service: UP" || echo " location-service: DOWN"
	@curl -sf http://localhost:3003/actuator/health 2>/dev/null && echo " matching-service: UP" || echo " matching-service: DOWN"
	@curl -sf http://localhost:3004/actuator/health 2>/dev/null && echo " auth-service: UP" || echo " auth-service: DOWN"
	@curl -sf http://localhost:3005/actuator/health 2>/dev/null && echo " payment-service: UP" || echo " payment-service: DOWN"
	@curl -sf http://localhost:3006/actuator/health 2>/dev/null && echo " pricing-service: UP" || echo " pricing-service: DOWN"
