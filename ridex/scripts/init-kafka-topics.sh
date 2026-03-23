#!/bin/bash
set -e

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:29092}"

echo "Creating Kafka topics..."

kafka-topics --create --if-not-exists --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic ridex.trip.events --partitions 16 --replication-factor 1

kafka-topics --create --if-not-exists --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic ridex.location.updates --partitions 32 --replication-factor 1

kafka-topics --create --if-not-exists --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic ridex.match.requests --partitions 16 --replication-factor 1

kafka-topics --create --if-not-exists --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic ridex.match.results --partitions 16 --replication-factor 1

kafka-topics --create --if-not-exists --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic ridex.payment.events --partitions 8 --replication-factor 1

kafka-topics --create --if-not-exists --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic ridex.notification.fanout --partitions 16 --replication-factor 1

echo "All Kafka topics created successfully."
kafka-topics --list --bootstrap-server "$BOOTSTRAP_SERVER"
