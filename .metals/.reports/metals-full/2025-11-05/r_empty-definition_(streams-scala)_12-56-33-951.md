file://<WORKSPACE>/streams-scala/src/main/scala/Main.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -TopicPartition.
	 -TopicPartition#
	 -TopicPartition().
	 -scala/Predef.TopicPartition.
	 -scala/Predef.TopicPartition#
	 -scala/Predef.TopicPartition().
offset: 6096
uri: file://<WORKSPACE>/streams-scala/src/main/scala/Main.scala
text:
```scala
package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

// 🔥 STRUCTURE COMPATIBLE AVEC CLICKHOUSE
type MessageEvent struct {
	MessageID      string    `json:"message_id"`
	ConversationID string    `json:"conversation_id"` // ✅ AJOUT
	SenderID       string    `json:"sender_id"`       // ✅ RENOMMÉ (était user_id)
	ReceiverID     string    `json:"receiver_id"`     // ✅ AJOUT
	Content        string    `json:"content"`
	Timestamp      time.Time `json:"timestamp"`
	MessageType    string    `json:"message_type"` // ✅ RENOMMÉ (était event_type)
	
	// Compatibilité descendante
	UserID    string `json:"user_id,omitempty"`    // ✅ GARDÉ pour compatibilité
	EventType string `json:"event_type,omitempty"` // ✅ GARDÉ pour compatibilité
}

type ViewEvent struct {
	MessageID string    `json:"message_id"`
	ViewerID  string    `json:"viewer_id"` // ✅ RENOMMÉ pour clarté
	Timestamp time.Time `json:"timestamp"`
	EventType string    `json:"event_type"`
}

var (
	users = []string{"alice", "bob", "charlie", "diana", "eve", "frank", "grace", "henry"}
	
	// Conversations réalistes avec paires d'utilisateurs
	conversations = []struct {
		ID     string
		User1  string
		User2  string
		Topic  string
	}{
		{"conv-work-1", "alice", "bob", "work"},
		{"conv-work-2", "charlie", "diana", "work"},
		{"conv-social-1", "eve", "frank", "social"},
		{"conv-social-2", "grace", "henry", "social"},
		{"conv-tech-1", "alice", "charlie", "tech"},
		{"conv-tech-2", "bob", "eve", "tech"},
		{"conv-random-1", "diana", "frank", "random"},
		{"conv-random-2", "grace", "henry", "random"},
	}

	topics = map[string][]string{
		"work": {
			"Need to schedule a meeting for the project",
			"Deadline for Q3 deliverables is approaching",
			"Let's discuss the new feature implementation",
			"Client presentation tomorrow at 2 PM",
			"Code review session this afternoon",
			"Project deadline extended to next week",
			"Team building event on Friday",
			"New project kickoff meeting scheduled",
		},
		"social": {
			"Anyone want to grab lunch later?",
			"Great party last night! Thanks everyone",
			"Check out this amazing restaurant I found",
			"Weekend plans anyone?",
			"Movie night at my place this Saturday",
			"Just tried that new coffee place, highly recommend!",
			"Birthday party next week, everyone invited",
			"Game night this Friday, who's in?",
		},
		"tech": {
			"Just tried the new Kafka version, amazing features!",
			"Anyone working with Scala 3? Thoughts?",
			"Kubernetes deployment strategies discussion",
			"Machine learning model performance optimization",
			"React vs Vue for new frontend project",
			"Microservices architecture best practices",
			"DevOps pipeline optimization techniques",
			"Cloud cost optimization strategies",
		},
		"random": {
			"Beautiful weather today!",
			"Just finished reading a great book",
			"Working from home today",
			"Traffic was terrible this morning",
			"Can't believe it's already October",
			"Need to remember to buy groceries",
			"Thinking about learning a new language",
			"Weekend can't come soon enough",
		},
	}
)

func main() {
	fmt.Println("🚀 Starting SMART Message Producer (Compatible Version)...")

	producer, err := kafka.NewProducer(&kafka.ConfigMap{
		"bootstrap.servers": "localhost:29092",
		"client.id":         "smart-message-producer",
		"partitioner":       "murmur2", // ✅ Partitionnement intelligent
	})
	if err != nil {
		log.Fatal("Failed to create producer:", err)
	}
	defer producer.Close()

	fmt.Println("✅ Connected to Kafka on port 29092!")

	// Gestion des erreurs de delivery
	go func() {
		for e := range producer.Events() {
			switch ev := e.(type) {
			case *kafka.Message:
				if ev.TopicPartition.Error != nil {
					fmt.Printf("❌ Delivery failed: %v\n", ev.TopicPartition.Error)
				}
			}
		}
	}()

	messageCount := 0
	conversationMessages := make(map[string]int)

	fmt.Printf("📨 Producing SMART messages with conversations...\n\n")

	for {
		// Choisir une conversation aléatoire
		conv := conversations[rand.Intn(len(conversations))]
		
		// Alterner entre les deux utilisateurs de la conversation
		var sender, receiver string
		if conversationMessages[conv.ID]%2 == 0 {
			sender, receiver = conv.User1, conv.User2
		} else {
			sender, receiver = conv.User2, conv.User1
		}

		content := topics[conv.Topic][rand.Intn(len(topics[conv.Topic]))]

		message := MessageEvent{
			MessageID:      fmt.Sprintf("msg-%d-%d", time.Now().Unix(), messageCount),
			ConversationID: conv.ID,           // ✅ CONVERSATION
			SenderID:       sender,            // ✅ EXPÉDITEUR
			ReceiverID:     receiver,          // ✅ DESTINATAIRE
			Content:        content,
			Timestamp:      time.Now(),
			MessageType:    "text",            // ✅ TYPE MESSAGE
			
			// Compatibilité
			UserID:    sender,                 // ✅ DOUBLE SET pour compatibilité
			EventType: "message_sent",         // ✅ DOUBLE SET pour compatibilité
		}

		value, _ := json.Marshal(message)
		
		// ✅ PRODUIRE AVEC CLÉ DE PARTITION = conversation_id
		err := producer.Produce(&kafka.Message{
			TopicPartition: kafka.TopicPartition{
				Topic:     &[]string{"chat.messages"}[0], // ✅ TOPIC CORRIGÉ
				Partition: kafka.PartitionAny,
			},
			Key:   []byte(conv.ID), // ✅ CLÉ POUR PARTITIONNEMENT
			Value: value,
		}, nil)

		if err != nil {
			fmt.Printf("❌ Failed: %v\n", err)
		} else {
			fmt.Printf("💬 [%s] %s → %s: %s\n", 
				conv.ID, message.SenderID, message.ReceiverID, message.Content)
		}

		conversationMessages[conv.ID]++

		// ✅ GÉNÉRATION D'ÉVÉNEMENTS "VU" CORRIGÉS
		if messageCount%3 == 0 && messageCount > 0 {
			viewer := users[rand.Intn(len(users))]
			// Prendre un message existant de cette conversation
			randomMessageId := fmt.Sprintf("msg-%d-%d", time.Now().Unix()-30, rand.Intn(messageCount)+1)

			viewEvent := ViewEvent{
				MessageID: randomMessageId,
				ViewerID:  viewer,
				Timestamp: time.Now(),
				EventType: "message_viewed",
			}
			viewValue, _ := json.Marshal(viewEvent)
			producer.Produce(&kafka.Message{
				TopicPart@@ition: kafka.TopicPartition{
					Topic:     &[]string{"message-views"}[0],
					Partition: kafka.PartitionAny,
				},
				Value: viewValue,
			}, nil)
			fmt.Printf("👀 %s viewed a message in conversation %s\n", viewer, conv.ID)
		}

		messageCount++
		time.Sleep(time.Millisecond * 1500)
	}
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 