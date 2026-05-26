#!/bin/bash
set -e

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-12}"
RETENTION_MS="${KAFKA_RETENTION_MS:-604800000}"
REPLICATION="${KAFKA_REPLICATION_FACTOR:-1}"

create_topic() {
  local topic=$1
  kafka-topics.sh --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions "$PARTITIONS" \
    --replication-factor "$REPLICATION" \
    --config retention.ms="$RETENTION_MS"
  echo "Topic ready: $topic (partitions=$PARTITIONS, retention.ms=$RETENTION_MS, rf=$REPLICATION)"
}

create_topic events.raw
create_topic events.processed
create_topic events.dlq

echo "All Kafka topics initialized."
