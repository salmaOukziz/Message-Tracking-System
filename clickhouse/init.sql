CREATE DATABASE IF NOT EXISTS message_db;

CREATE TABLE IF NOT EXISTS message_db.messages (
    message_id String,
    conversation_id String,
    sender_id String,
    receiver_id String,
    content String,
    created_at DateTime64(3),
    message_type String DEFAULT 'text',
    user_id String DEFAULT '',
    event_type String DEFAULT 'message_sent',
    read_status String DEFAULT 'sent',      -- Nouveau: sent, delivered, read
    read_at DateTime64(3) DEFAULT NULL      -- Nouveau: timestamp de lecture
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (conversation_id, created_at, message_id);

-- Table pour les statistiques utilisateur
CREATE TABLE IF NOT EXISTS message_db.user_stats (
    user_id String,
    message_count UInt32,
    active_conversations UInt16,
    last_active DateTime64(3),
    created_at DateTime64(3) DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (user_id, created_at);

-- Table pour les métriques de conversation
CREATE TABLE IF NOT EXISTS message_db.conversation_metrics (
    conversation_id String,
    message_count UInt32,
    participant_count UInt16,
    last_message_at DateTime64(3),
    created_at DateTime64(3) DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (conversation_id, created_at);

-- Vue matérialisée pour les stats en temps réel
CREATE MATERIALIZED VIEW IF NOT EXISTS message_db.realtime_user_activity
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (user_id, created_at)
AS SELECT
    sender_id as user_id,
    toStartOfMinute(created_at) as created_at,
    count(*) as messages_last_minute
FROM message_db.messages
GROUP BY user_id, created_at;

-- Index pour les recherches par utilisateur
ALTER TABLE message_db.messages ADD INDEX user_id_index (user_id) TYPE minmax GRANULARITY 1;
ALTER TABLE message_db.messages ADD INDEX sender_index (sender_id) TYPE minmax GRANULARITY 1;
ALTER TABLE message_db.messages ADD INDEX receiver_index (receiver_id) TYPE minmax GRANULARITY 1;
ALTER TABLE message_db.messages ADD INDEX conversation_index (conversation_id) TYPE minmax GRANULARITY 1;

SELECT '✅ message_db initialized successfully with enhanced schema!' as status;