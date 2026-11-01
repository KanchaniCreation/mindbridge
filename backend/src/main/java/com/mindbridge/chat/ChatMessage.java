package com.mindbridge.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    /** Chosen by the client. Sending the same one twice never creates a second message. */
    @Column(name = "client_message_id", nullable = false, updatable = false)
    private UUID clientMessageId;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
        // for JPA
    }

    public ChatMessage(UUID chatId, UUID senderId, UUID clientMessageId, String body) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getChatId() {
        return chatId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
