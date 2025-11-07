package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"time"

	"github.com/IBM/sarama"
)

type MessageEvent struct {
	MessageID      string    `json:"message_id"`
	UserID         string    `json:"user_id"`
	Content        string    `json:"content"`
	Timestamp      time.Time `json:"timestamp"`
	EventType      string    `json:"event_type"`
	ConversationID string    `json:"conversation_id,omitempty"`
	SenderID       string    `json:"sender_id,omitempty"`
	ReceiverID     string    `json:"receiver_id,omitempty"`
	MessageType    string    `json:"message_type,omitempty"`
}

type MessageViewEvent struct {
	MessageID string    `json:"message_id"`
	UserID    string    `json:"user_id"`
	ViewedAt  time.Time `json:"viewed_at"`
	Device    string    `json:"device"`
}

var (
	users = []string{"alice", "bob", "charlie", "diana", "eve", "frank", "grace", "henry"}

	topics = map[string][]string{
		"work": {
			"Need to schedule a meeting for the project",
			"Deadline for Q3 deliverables is approaching",
			"Let's discuss the new feature implementation",
			"Client presentation tomorrow at 2 PM",
			"Code review session this afternoon",
		},
		"social": {
			"Anyone want to grab lunch later?",
			"Great party last night! Thanks everyone",
			"Check out this amazing restaurant I found",
			"Weekend plans anyone?",
			"Movie night at my place this Saturday",
		},
		"tech": {
			"Just tried the new Kafka version, amazing features!",
			"Anyone working with Scala 3? Thoughts?",
			"Kubernetes deployment strategies discussion",
			"Machine learning model performance optimization",
			"React vs Vue for new frontend project",
		},
	}

	// Stocker les derniers messages pour les marquer comme lus
	recentMessages []string
)

func main() {
	fmt.Println("🚀 Starting Kafka Producer with Sarama...")

	config := sarama.NewConfig()
	config.Producer.Return.Successes = true
	config.Producer.RequiredAcks = sarama.WaitForAll

	producer, err := sarama.NewSyncProducer([]string{"kafka:9092"}, config)
	if err != nil {
		log.Fatal("Failed to create producer:", err)
	}
	defer producer.Close()

	fmt.Println("✅ Connected to Kafka!")
	fmt.Printf("📨 Producing messages to Kafka...\n\n")

	messageCount := 0
	topicKeys := []string{"work", "social", "tech"}

	// Goroutine pour nettoyer les anciens messages
	go cleanupRecentMessages()

	for {
		topicKey := topicKeys[rand.Intn(len(topicKeys))]
		content := topics[topicKey][rand.Intn(len(topics[topicKey]))]

		// CORRECTION : S'assurer que user_id = sender_id pour la cohérence
		sender := users[rand.Intn(len(users))]
		receiver := users[rand.Intn(len(users))]

		// Éviter que l'expéditeur et le destinataire soient la même personne
		for sender == receiver {
			receiver = users[rand.Intn(len(users))]
		}

		message := MessageEvent{
			MessageID:      fmt.Sprintf("msg-%d-%d", time.Now().Unix(), messageCount),
			UserID:         sender, // CORRIGÉ : user_id = sender_id
			Content:        content,
			Timestamp:      time.Now(),
			EventType:      "message_sent",
			ConversationID: fmt.Sprintf("conv-%d", rand.Intn(5)+1),
			SenderID:       sender,   // Expéditeur
			ReceiverID:     receiver, // Destinataire
			MessageType:    "text",
		}

		value, _ := json.Marshal(message)

		msg := &sarama.ProducerMessage{
			Topic: "chat.messages",
			Key:   sarama.StringEncoder(message.ConversationID),
			Value: sarama.StringEncoder(value),
		}

		partition, offset, err := producer.SendMessage(msg)
		if err != nil {
			fmt.Printf("❌ Failed to send message: %v\n", err)
		} else {
			fmt.Printf("💬 [%s] %s → %s: %s (partition: %d, offset: %d)\n",
				message.ConversationID, message.SenderID, message.ReceiverID,
				message.Content, partition, offset)

			// Ajouter le message à la liste des messages récents
			recentMessages = append(recentMessages, message.MessageID)
		}

		// Simuler des vues de messages
		if rand.Intn(3) == 0 { // 33% de chance de marquer un message comme lu
			go simulateMessageView(producer, message.MessageID, message.ReceiverID)
		}

		// Simuler des vues de messages anciens (10% de chance)
		if rand.Intn(10) == 0 && len(recentMessages) > 0 {
			go simulateRandomMessageView(producer)
		}

		messageCount++
		time.Sleep(2 * time.Second)
	}
}

// Fonction pour envoyer les vues de messages
func sendMessageView(producer sarama.SyncProducer, messageID string, userID string) {
	viewEvent := MessageViewEvent{
		MessageID: messageID,
		UserID:    userID,
		ViewedAt:  time.Now(),
		Device:    "web",
	}

	value, _ := json.Marshal(viewEvent)

	msg := &sarama.ProducerMessage{
		Topic: "message-views",
		Key:   sarama.StringEncoder(userID),
		Value: sarama.StringEncoder(value),
	}

	_, _, err := producer.SendMessage(msg)
	if err != nil {
		fmt.Printf("❌ Failed to send message view: %v\n", err)
	} else {
		fmt.Printf("👀 Message view: %s read by %s\n", messageID, userID)
	}
}

// Simuler la vue d'un message après un délai aléatoire
func simulateMessageView(producer sarama.SyncProducer, messageID string, userID string) {
	// Attendre un délai aléatoire entre 1 et 10 secondes
	delay := time.Duration(rand.Intn(10)+1) * time.Second
	time.Sleep(delay)

	sendMessageView(producer, messageID, userID)
}

// Simuler la vue d'un message aléatoire parmi les messages récents
func simulateRandomMessageView(producer sarama.SyncProducer) {
	if len(recentMessages) == 0 {
		return
	}

	// Choisir un message aléatoire
	randomMessageID := recentMessages[rand.Intn(len(recentMessages))]
	randomUser := users[rand.Intn(len(users))]

	// Simuler la vue après un délai
	go func(msgID string, user string) {
		delay := time.Duration(rand.Intn(15)+5) * time.Second
		time.Sleep(delay)
		sendMessageView(producer, msgID, user)
	}(randomMessageID, randomUser)
}

// Nettoyer les anciens messages de la liste des messages récents
func cleanupRecentMessages() {
	for {
		time.Sleep(30 * time.Second)

		// Garder seulement les 50 derniers messages
		if len(recentMessages) > 50 {
			recentMessages = recentMessages[len(recentMessages)-50:]
			fmt.Printf("🧹 Cleaned up recent messages, keeping last 50\n")
		}
	}
}
