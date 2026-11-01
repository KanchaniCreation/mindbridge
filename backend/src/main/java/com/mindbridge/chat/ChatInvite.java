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

/** A single-use, short-lived invitation. Only a hash of the code is stored. */
@Entity
@Table(name = "chat_invites")
public class ChatInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private UUID creatorId;

    @Column(name = "code_hash", nullable = false, updatable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatInvite() {
        // for JPA
    }

    public ChatInvite(UUID creatorId, String codeHash, Instant expiresAt) {
        this.creatorId = creatorId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markUsed(Instant at) {
        this.usedAt = at;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
