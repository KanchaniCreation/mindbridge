package com.mindbridge.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 60)
    private String displayName;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    /** Self-declared "I am 18 or older", required before using chat. Not a verification. */
    @Column(name = "adult_confirmed_at")
    private Instant adultConfirmedAt;

    /** The only name other people see while matching and chatting. Unique, case-insensitive. */
    @Column(length = 20)
    private String alias;

    @Column(length = 140)
    private String about;

    /** Never shown to anyone until both people agree to reveal it. */
    @Column(name = "real_name", length = 60)
    private String realName;

    /** Comma-separated topic names, for example "SLEEP,STUDY". Parsed by the match module. */
    @Column(name = "topics", nullable = false, length = 200)
    private String topics = "";

    @Column(name = "open_to_chat", nullable = false)
    private boolean openToChat;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // for JPA
    }

    public User(String email, String passwordHash, String displayName, Instant termsAcceptedAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.termsAcceptedAt = termsAcceptedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getAdultConfirmedAt() {
        return adultConfirmedAt;
    }

    public void confirmAdult(Instant at) {
        if (this.adultConfirmedAt == null) {
            this.adultConfirmedAt = at;
        }
    }

    public String getAlias() {
        return alias;
    }

    public String getAbout() {
        return about;
    }

    public String getRealName() {
        return realName;
    }

    public String getTopicsCsv() {
        return topics;
    }

    public boolean isOpenToChat() {
        return openToChat;
    }

    public void updateMatchProfile(String alias, String about, String realName, String topicsCsv,
                                   boolean openToChat) {
        this.alias = alias;
        this.about = about;
        this.realName = realName;
        this.topics = topicsCsv == null ? "" : topicsCsv;
        this.openToChat = openToChat;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
