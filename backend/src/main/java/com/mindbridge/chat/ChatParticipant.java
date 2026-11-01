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

/** Links a real user to a chat under a pseudonym that is different in every chat. */
@Entity
@Table(name = "chat_participants")
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String pseudonym;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected ChatParticipant() {
        // for JPA
    }

    public ChatParticipant(UUID chatId, UUID userId, String pseudonym) {
        this.chatId = chatId;
        this.userId = userId;
        this.pseudonym = pseudonym;
    }

    @PrePersist
    void onCreate() {
        this.joinedAt = Instant.now();
    }

    public UUID getChatId() {
        return chatId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPseudonym() {
        return pseudonym;
    }
}
