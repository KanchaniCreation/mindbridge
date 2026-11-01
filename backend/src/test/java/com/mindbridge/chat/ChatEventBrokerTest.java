package com.mindbridge.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatEventBrokerTest {

    private final ChatEventBroker broker = new ChatEventBroker();
    private final UUID chatId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void everyListenerGetsTheEventFromTheirOwnPointOfView() {
        List<Object> aliceGot = new ArrayList<>();
        List<Object> bobGot = new ArrayList<>();
        broker.subscribe(chatId, alice, (event, payload) -> aliceGot.add(event + ":" + payload));
        broker.subscribe(chatId, bob, (event, payload) -> bobGot.add(event + ":" + payload));

        broker.publish(chatId, "message", viewer -> viewer.equals(alice) ? "mine" : "theirs");

        assertThat(aliceGot).containsExactly("message:mine");
        assertThat(bobGot).containsExactly("message:theirs");
    }

    @Test
    void unsubscribedListenersStopReceivingEvents() {
        List<Object> got = new ArrayList<>();
        Runnable unsubscribe = broker.subscribe(chatId, alice, (event, payload) -> got.add(payload));

        unsubscribe.run();
        broker.publish(chatId, "message", viewer -> "x");

        assertThat(got).isEmpty();
        assertThat(broker.subscriberCount(chatId)).isZero();
    }

    @Test
    void aBrokenConnectionIsRemovedWithoutAffectingOthers() {
        List<Object> got = new ArrayList<>();
        broker.subscribe(chatId, alice, (event, payload) -> {
            throw new IOException("connection closed");
        });
        broker.subscribe(chatId, bob, (event, payload) -> got.add(payload));

        broker.publish(chatId, "message", viewer -> "hello");

        assertThat(got).containsExactly("hello");
        assertThat(broker.subscriberCount(chatId)).isEqualTo(1);
    }

    @Test
    void eventsForOtherChatsAreNotDelivered() {
        List<Object> got = new ArrayList<>();
        broker.subscribe(chatId, alice, (event, payload) -> got.add(payload));

        broker.publish(UUID.randomUUID(), "message", viewer -> "other chat");

        assertThat(got).isEmpty();
    }
}
