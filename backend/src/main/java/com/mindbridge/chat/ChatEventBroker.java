package com.mindbridge.chat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Keeps track of who is listening to which chat and pushes live events to them.
 * In memory, so it works for one application instance. Several instances would need a shared
 * broker (documented as a known limitation).
 */
@Component
public class ChatEventBroker {

    private record Registration(UUID userId, ChatSubscriber subscriber) {
    }

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<Registration>> byChat = new ConcurrentHashMap<>();

    /** Returns a Runnable that removes this subscription. */
    public Runnable subscribe(UUID chatId, UUID userId, ChatSubscriber subscriber) {
        Registration registration = new Registration(userId, subscriber);
        byChat.computeIfAbsent(chatId, id -> new CopyOnWriteArrayList<>()).add(registration);
        return () -> remove(chatId, registration);
    }

    /**
     * Sends an event to everyone listening to the chat. The payload is built per listener, so each
     * person sees the message from their own point of view ("mine" or not).
     */
    public void publish(UUID chatId, String event, Function<UUID, Object> payloadFor) {
        List<Registration> registrations = byChat.get(chatId);
        if (registrations == null) {
            return;
        }
        for (Registration registration : registrations) {
            try {
                registration.subscriber().send(event, payloadFor.apply(registration.userId()));
            } catch (IOException | RuntimeException ex) {
                remove(chatId, registration);
            }
        }
    }

    public int subscriberCount(UUID chatId) {
        List<Registration> registrations = byChat.get(chatId);
        return registrations == null ? 0 : registrations.size();
    }

    private void remove(UUID chatId, Registration registration) {
        byChat.computeIfPresent(chatId, (id, list) -> {
            list.remove(registration);
            return list.isEmpty() ? null : list;
        });
    }
}
