package com.mindbridge.chat;

import java.io.IOException;

/** Something that can receive live events for one person in one chat (an open SSE connection). */
@FunctionalInterface
public interface ChatSubscriber {

    void send(String event, Object payload) throws IOException;
}
