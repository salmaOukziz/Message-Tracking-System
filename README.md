```markdown
#  Message Tracking System

##  Overview
A distributed, real-time message tracking and analytics platform built with **Kafka**, **ClickHouse**, and **Kafka Streams**. It processes, analyzes, and stores message events while providing complete observability through **Grafana**.

---

##  Architecture
```

┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   Producer Go   │───▶│   Kafka Topics   │───▶│  Kafka Streams   │
│  (Message Gen)  │    │                  │    │   Processors     │
└─────────────────┘    └──────────────────┘    └──────────────────┘
│
▼
┌──────────────────┐
│   ClickHouse     │
│   (Storage)      │
└──────────────────┘
│
┌──────────────────┐
│    Grafana       │
│  (Monitoring)    │
└──────────────────┘

````

---

##  Kafka Topics

| Topic | Partitions | Retention | Purpose |
|-------|-------------|------------|----------|
| chat.messages | 6 | 7 days | Raw message events |
| message-views | 4 | 30 days | Message read receipts |
| conversation-metrics | 3 | - | Conversation analytics |
| trending-words | 3 | - | Word frequency trends |
| user-activity-metrics | 3 | - | User behavior metrics |
| anomaly-alerts | 2 | - | Spam detection alerts |

---

##  Stream Processing

### 1. ClickHouse Sink (`streams-clickhouse-sink`)
- Persists data into ClickHouse  
- Updates message status and timestamps  
- Maintains conversation history  

### 2. Conversation Analytics (`streams-conversation-analytics`)
- Tracks message volume per conversation  
- Sliding windows (30s)  
- Monitors activity peaks  

### 3. Trend Detection (`streams-trend-detection`)
- Extracts message content  
- Tracks trending words (>3 chars)  
- 2-minute analysis window  

### 4. Anomaly Detection (`streams-anomaly-detection`)
- Monitors user message frequency  
- Detects spam (≥8 messages/min)  
- Emits alerts  

---

##  Quick Start

### Prerequisites
- Docker & Docker Compose  
- 4GB+ RAM  

### Setup
```bash
git clone <repository>
cd message-tracking-system
docker-compose up -d
./scripts/create-topics.sh
````

### Access

* Kafka UI → [http://localhost:28080](http://localhost:28080)
* Grafana → [http://localhost:33000](http://localhost:33000) (admin/admin)
* ClickHouse → [http://localhost:18123](http://localhost:18123)
* REST API → [http://localhost:8080](http://localhost:8080)

---

##  Data Flow

**Flow:**

1. Producer sends events → `chat.messages`
2. View receipts → `message-views`
3. Kafka Streams process → ClickHouse + Metrics + Alerts

---

## 🗄️ Database Schema

```sql
CREATE TABLE messages (
    message_id String,
    conversation_id String,
    sender_id String,
    receiver_id String,
    content String,
    created_at DateTime64(3),
    message_type String DEFAULT 'text',
    user_id String DEFAULT '',
    event_type String DEFAULT 'message_sent',
    read_status String DEFAULT 'sent',
    read_at DateTime64(3) DEFAULT NULL
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (conversation_id, created_at, message_id);
```

**Supporting Tables:**

* `user_stats`
* `conversation_metrics`
* `realtime_user_activity`

---

## 🔍 Monitoring

### Grafana Dashboards

* Message volume
* User activity heatmaps
* Read rates
* Conversation metrics

### Kafka UI

* Topic exploration
* Consumer monitoring
* Offset tracking

---

## 🛠️ Development

**Structure**

```
message-tracking-system/
├── docker-compose.yml
├── scripts/
├── producer-go/
├── streams-clickhouse-sink/
├── streams-conversation-analytics/
├── streams-trend-detection/
├── streams-anomaly-detection/
├── api-rest/
├── clickhouse/
└── grafana/
```

**New Processor Example**

```scala
object NewProcessorApp extends App {
  val builder = new StreamsBuilder()
  builder.stream[String, String]("chat.messages")
    .foreach { (key, value) => /* logic */ }
}
```

---

##  Example Queries

```sql
-- Hourly message volume
SELECT toStartOfHour(created_at) AS hour, count(*) AS messages
FROM message_db.messages GROUP BY hour ORDER BY hour DESC;

-- Read rate per sender
SELECT sender_id,
       count(*) AS sent,
       countIf(read_status = 'read') AS read_count,
       round(read_count * 100.0 / sent, 2) AS read_rate
FROM message_db.messages GROUP BY sender_id;
```

---

##  Troubleshooting

```bash
# Restart unhealthy components
docker-compose restart kafka clickhouse

# Monitor logs
docker-compose logs -f [service]

# Health check
./scripts/health-check.sh
```

---

##  Configuration

| Variable            | Description     |
| ------------------- | --------------- |
| KAFKA_BROKERS       | kafka:9092      |
| CLICKHOUSE_URL      | clickhouse:8123 |
| CLICKHOUSE_USER     | admin           |
| CLICKHOUSE_PASSWORD | password        |

**Tuning:**

* Kafka: increase partitions
* ClickHouse: optimize merge trees
* Streams: enable exactly-once

---

##  Scaling

* Add Kafka brokers
* Scale stream processors
* Increase partitions

---

##  Contributing

1. Fork repo
2. Create feature branch
3. Add tests
4. Open PR

---

##  License

MIT License – see `LICENSE`

---

##  Support

* Check troubleshooting
* Review logs
* Verify Kafka/ClickHouse status

```
docker-compose ps
```

```
```
