package com.mindbridge.chat;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    Optional<ChatParticipant> findByChatIdAndUserId(UUID chatId, UUID userId);

    List<ChatParticipant> findByChatId(UUID chatId);

    List<ChatParticipant> findByUserId(UUID userId);

    List<ChatParticipant> findByChatIdIn(Collection<UUID> chatIds);
}
