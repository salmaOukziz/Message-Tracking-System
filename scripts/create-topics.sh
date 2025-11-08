#!/bin/bash

set -e

echo "🔧 Creating essential Kafka topics..."

KAFKA_HOST="kafka:9092"
MAX_RETRIES=15
RETRY_COUNT=0

# Wait for Kafka
echo " Waiting for Kafka to be ready..."
until kafka-topics --bootstrap-server $KAFKA_HOST --list > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo " Kafka not ready after $MAX_RETRIES retries. Exiting."
        exit 1
    fi
    echo "   Waiting for Kafka... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 4
done

echo "✅ Kafka is ready! Creating topics..."

# Function to create topic
create_topic() {
    local topic=$1
    local partitions=$2
    local retention_ms=${3:-"604800000"}  # 7 days default
    
    echo "📝 Creating topic: $topic"
    
    kafka-topics --bootstrap-server $KAFKA_HOST \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor 1 \
        --config retention.ms=$retention_ms \
        --config cleanup.policy=delete
        
    echo "    $topic created with $partitions partitions"
}

# ONLY topics actually used by your system
create_topic "chat.messages" 4 "604800000"          # Used by producer + all services
create_topic "message-views" 3 "2592000000"         # Used by producer for read receipts

echo ""
echo " Essential topics created successfully!"
echo ""
echo " Final topics list:"
kafka-topics --bootstrap-server $KAFKA_HOST --list
