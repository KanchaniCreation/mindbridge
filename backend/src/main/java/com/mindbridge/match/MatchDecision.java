package com.mindbridge.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One person's private answer about one other person. The other person never sees it. */
@Entity
@Table(name = "match_decisions")
public class MatchDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private Decision decision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MatchDecision() {
        // for JPA
    }

    public MatchDecision(UUID actorId, UUID targetId, Decision decision) {
        this.actorId = actorId;
        this.targetId = targetId;
        this.decision = decision;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
