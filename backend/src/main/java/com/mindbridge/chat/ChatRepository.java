package com.mindbridge.chat;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /** Used when an account is deleted: the other person's chat is closed, not silently left open. */
    @Modifying
    @Query(value = """
        UPDATE chats SET status = 'CLOSED', closed_at = now()
        WHERE status = 'ACTIVE'
          AND id IN (SELECT chat_id FROM chat_participants WHERE user_id = :userId)
        """, nativeQuery = true)
    int closeChatsOf(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "DELETE FROM chat_participants WHERE user_id = :userId", nativeQuery = true)
    int removeParticipant(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
        DELETE FROM chats c
        WHERE NOT EXISTS (SELECT 1 FROM chat_participants p WHERE p.chat_id = c.id)
        """, nativeQuery = true)
    int deleteChatsWithoutParticipants();

    /** How many open chats a person has right now (used for the "at most 5" rule). */
    @Query(value = """
        SELECT count(*) FROM chats c
        JOIN chat_participants p ON p.chat_id = c.id
        WHERE p.user_id = :userId AND c.status = 'ACTIVE'
        """, nativeQuery = true)
    long countActiveChatsOf(@Param("userId") UUID userId);
}
