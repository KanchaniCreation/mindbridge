package com.mindbridge.mood;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MoodEntryRepository extends JpaRepository<MoodEntry, UUID> {

    Optional<MoodEntry> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        select e from MoodEntry e
        where e.userId = :userId and e.recordedAt >= :from and e.recordedAt <= :to
        order by e.recordedAt desc
        """)
    Page<MoodEntry> findInRange(@Param("userId") UUID userId, @Param("from") Instant from,
                                @Param("to") Instant to, Pageable pageable);

    @Query("""
        select e from MoodEntry e join e.tags t
        where e.userId = :userId and t = :tag and e.recordedAt >= :from and e.recordedAt <= :to
        order by e.recordedAt desc
        """)
    Page<MoodEntry> findInRangeWithTag(@Param("userId") UUID userId, @Param("tag") String tag,
                                       @Param("from") Instant from, @Param("to") Instant to,
                                       Pageable pageable);

    @Query("""
        select e from MoodEntry e
        where e.userId = :userId and e.recordedAt >= :from and e.recordedAt <= :to
        order by e.recordedAt asc
        """)
    List<MoodEntry> findAllInRange(@Param("userId") UUID userId, @Param("from") Instant from,
                                   @Param("to") Instant to);
}
