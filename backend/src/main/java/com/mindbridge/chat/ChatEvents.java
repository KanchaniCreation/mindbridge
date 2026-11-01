package com.mindbridge.chat;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Turns committed changes into live events, so nobody is told about something that was rolled back. */
@Component
public class ChatEvents {

    private final ChatEventBroker broker;

    public ChatEvents(ChatEventBroker broker) {
        this.broker = broker;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onMessageSent(ChatMessageSent event) {
        broker.publish(event.chatId(), "message", viewerId -> new ChatMessageResponse(
            event.messageId(),
            viewerId.equals(event.senderId()),
            event.pseudonyms().getOrDefault(event.senderId(), ChatService.FORMER_PARTICIPANT),
            event.body(),
            event.createdAt()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onChatClosed(ChatClosed event) {
        broker.publish(event.chatId(), "closed", viewerId -> Map.of("chatId", event.chatId()));
    }
}
