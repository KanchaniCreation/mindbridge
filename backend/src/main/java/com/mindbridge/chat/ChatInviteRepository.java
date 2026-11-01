package com.mindbridge.chat;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatInviteRepository extends JpaRepository<ChatInvite, UUID> {

    /** Row lock: two people redeeming the same code at the same moment cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ChatInvite i where i.codeHash = :hash")
    Optional<ChatInvite> findByCodeHashForUpdate(@Param("hash") String hash);

    long countByCreatorIdAndUsedAtIsNullAndExpiresAtAfter(UUID creatorId, Instant now);
}
