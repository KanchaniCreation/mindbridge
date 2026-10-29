package com.mindbridge.journal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    /** Ownership is part of the lookup itself, so another user's id simply finds nothing. */
    Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Search within ONE user's entries. Empty strings mean "no filter" (avoids null-parameter
     * typing problems on PostgreSQL). All values are bound parameters, never concatenated into SQL.
     */
    @Query("""
        select e from JournalEntry e
        where e.userId = :userId
          and (:category = '' or lower(e.category) = :category)
          and (:q = '' or lower(e.title) like :pattern or lower(e.body) like :pattern)
        order by e.createdAt desc
        """)
    Page<JournalEntry> search(@Param("userId") UUID userId,
                              @Param("category") String category,
                              @Param("q") String q,
                              @Param("pattern") String pattern,
                              Pageable pageable);
}
