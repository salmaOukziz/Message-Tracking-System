package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"
)

type HealthResponse struct {
	Status    string `json:"status"`
	Service   string `json:"service"`
	Timestamp string `json:"timestamp"`
}

type MessageRequest struct {
	ConversationID string `json:"conversation_id"`
	SenderID       string `json:"sender_id"`
	ReceiverID     string `json:"receiver_id"`
	Content        string `json:"content"`
}

type MessageResponse struct {
	Status    string `json:"status"`
	MessageID string `json:"message_id"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	log.Println(" Health check called")

	response := HealthResponse{
		Status:    "healthy",
		Service:   "message-api",
		Timestamp: time.Now().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func sendMessageHandler(w http.ResponseWriter, r *http.Request) {
	var req MessageRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	log.Printf(" Message: %s → %s: %s", req.SenderID, req.ReceiverID, req.Content)

	messageID := fmt.Sprintf("msg-%d", time.Now().UnixNano())
	response := MessageResponse{
		Status:    "success",
		MessageID: messageID,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func main() {
	fmt.Println(" Starting Go Message API...")

	http.HandleFunc("/api/v1/health", healthHandler)
	http.HandleFunc("/api/v1/messages/send", sendMessageHandler)
	http.HandleFunc("/api/v1/messages/conversation/", func(w http.ResponseWriter, r *http.Request) {
		// Extraire l'ID de conversation de l'URL
		conversationID := r.URL.Path[len("/api/v1/messages/conversation/"):]
		response := map[string]interface{}{
			"conversation_id": conversationID,
			"messages":        []string{},
		}
		json.NewEncoder(w).Encode(response)
	})

	port := ":8080"
	fmt.Printf(" Server starting on http://localhost%s\n", port)
	fmt.Println(" Available endpoints:")
	fmt.Println("  GET  /api/v1/health")
	fmt.Println("  POST /api/v1/messages/send")
	fmt.Println("  GET  /api/v1/messages/conversation/{id}")

	log.Fatal(http.ListenAndServe(port, nil))
}
