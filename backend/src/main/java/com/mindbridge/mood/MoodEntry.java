package com.mindbridge.mood;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "mood_entries")
public class MoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "mood_value", nullable = false)
    private int moodValue;

    @Column(length = 1000)
    private String note;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mood_entry_emotions", joinColumns = @JoinColumn(name = "mood_entry_id"))
    @Column(name = "emotion", nullable = false, length = 40)
    @BatchSize(size = 50)
    private Set<String> emotions = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "mood_entry_tags", joinColumns = @JoinColumn(name = "mood_entry_id"))
    @Column(name = "tag", nullable = false, length = 40)
    @BatchSize(size = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected MoodEntry() {
        // for JPA
    }

    public MoodEntry(UUID userId, int moodValue, Set<String> emotions, Set<String> tags,
                     String note, Instant recordedAt) {
        this.userId = userId;
        this.moodValue = moodValue;
        this.emotions = new LinkedHashSet<>(emotions);
        this.tags = new LinkedHashSet<>(tags);
        this.note = note;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getMoodValue() {
        return moodValue;
    }

    public String getNote() {
        return note;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public Set<String> getEmotions() {
        return emotions;
    }

    public Set<String> getTags() {
        return tags;
    }
}
