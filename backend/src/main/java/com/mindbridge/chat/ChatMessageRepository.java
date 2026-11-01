package com.mindbridge.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable pageable);

    Optional<ChatMessage> findByChatIdAndSenderIdAndClientMessageId(UUID chatId, UUID senderId,
                                                                   UUID clientMessageId);

    Optional<ChatMessage> findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);
}
