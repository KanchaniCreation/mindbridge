package com.mindbridge.chat;

import com.mindbridge.user.AccountDeleting;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Runs inside the account-deletion transaction. The leaving person's messages disappear (the
 * database cascades them), the other person's chat is closed, and chats nobody belongs to are removed.
 */
@Component
public class ChatAccountCleanup {

    private final ChatRepository chats;

    public ChatAccountCleanup(ChatRepository chats) {
        this.chats = chats;
    }

    @EventListener
    void onAccountDeleting(AccountDeleting event) {
        chats.closeChatsOf(event.userId());
        chats.removeParticipant(event.userId());
        chats.deleteChatsWithoutParticipants();
    }
}
