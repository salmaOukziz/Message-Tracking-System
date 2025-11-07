# scripts/create-topics.sh
#!/bin/bash

set -e  # Stop at the first error

echo " Creating Kafka topics for 100% compatible system..."

KAFKA_HOST="kafka:9092"
MAX_RETRIES=15
RETRY_COUNT=0

# Waiting for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
until kafka-topics --bootstrap-server $KAFKA_HOST --list > /dev/null 2>&1; do
    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo " Kafka not ready after $MAX_RETRIES retries. Exiting."
        exit 1
    fi
    echo " Waiting for Kafka... (attempt $RETRY_COUNT/$MAX_RETRIES)"
    sleep 4
done

echo " Kafka is ready! Creating topics..."

# Fonction pour créer un topic avec gestion d'erreur
create_topic() {
    local topic=$1
    local partitions=$2
    local retention_ms=${3:-""}
    local cleanup_policy=${4:-"delete"}
    
    echo "Creating topic: $topic"
    
    local config_args=""
    if [ -n "$retention_ms" ]; then
        config_args="--config retention.ms=$retention_ms --config cleanup.policy=$cleanup_policy"
    fi
    
    kafka-topics --bootstrap-server $KAFKA_HOST \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor 1 \
        $config_args
        
    echo "   $topic created with $partitions partitions"
}

# Create topics with the optimal configuration
create_topic "chat.messages" 6 "604800000" "delete"
create_topic "message-views" 4 "2592000000" "compact"
create_topic "conversation-metrics" 3
create_topic "trending-words" 3
create_topic "user-activity-metrics" 3
create_topic "anomaly-alerts" 2

echo ""
echo " All topics created successfully!"
echo ""
echo " Final topics list:"
kafka-topics --bootstrap-server $KAFKA_HOST --list