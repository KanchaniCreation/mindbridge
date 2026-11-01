package com.mindbridge.chat;

import com.mindbridge.common.PageResponse;
import com.mindbridge.security.AppUserDetails;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ChatService chat;
    private final ChatEventBroker broker;

    public ChatController(ChatService chat, ChatEventBroker broker) {
        this.chat = chat;
        this.broker = broker;
    }

    @PostMapping("/chat/adult-confirmation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmAdult(@AuthenticationPrincipal AppUserDetails me) {
        chat.confirmAdult(me.getId());
    }

    @PostMapping("/chat/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse createInvite(@AuthenticationPrincipal AppUserDetails me) {
        return chat.createInvite(me.getId());
    }

    @PostMapping("/chat/invites/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSummaryResponse redeem(@AuthenticationPrincipal AppUserDetails me,
                                      @Valid @RequestBody RedeemInviteRequest request) {
        return chat.redeemInvite(me.getId(), request.code());
    }

    @GetMapping("/chats")
    public List<ChatSummaryResponse> list(@AuthenticationPrincipal AppUserDetails me) {
        return chat.listChats(me.getId());
    }

    @GetMapping("/chats/{id}/messages")
    public PageResponse<ChatMessageResponse> messages(@AuthenticationPrincipal AppUserDetails me,
                                                      @PathVariable("id") UUID id,
                                                      @RequestParam(name = "page", defaultValue = "0") int page,
                                                      @RequestParam(name = "size", defaultValue = "30") int size) {
        return chat.getMessages(me.getId(), id, page, size);
    }

    @PostMapping("/chats/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@AuthenticationPrincipal AppUserDetails me,
                                    @PathVariable("id") UUID id,
                                    @Valid @RequestBody MessageRequest request) {
        return chat.sendMessage(me.getId(), id, request);
    }

    /** Live updates (Server-Sent Events). Only members get a stream; everyone else gets 404. */
    @GetMapping("/chats/{id}/stream")
    public SseEmitter stream(@AuthenticationPrincipal AppUserDetails me, @PathVariable("id") UUID id) {
        chat.requireMembership(me.getId(), id);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Runnable unsubscribe = broker.subscribe(id, me.getId(),
            (event, payload) -> emitter.send(SseEmitter.event().name(event).data(payload)));
        emitter.onCompletion(unsubscribe);
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> unsubscribe.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException ex) {
            unsubscribe.run();
        }
        return emitter;
    }

    @PostMapping("/chats/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@AuthenticationPrincipal AppUserDetails me, @PathVariable("id") UUID id) {
        chat.close(me.getId(), id);
    }

    @PostMapping("/chats/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(@AuthenticationPrincipal AppUserDetails me, @PathVariable("id") UUID id,
                       @Valid @RequestBody ReportRequest request) {
        chat.report(me.getId(), id, request);
    }
}
