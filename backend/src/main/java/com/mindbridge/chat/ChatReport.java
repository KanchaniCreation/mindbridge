package com.mindbridge.chat;

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

/** A report stores the reason only. It does not copy any message text. */
@Entity
@Table(name = "chat_reports")
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ReportReason reason;

    @Column(length = 500, updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatReport() {
        // for JPA
    }

    public ChatReport(UUID chatId, UUID reporterId, ReportReason reason, String note) {
        this.chatId = chatId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
