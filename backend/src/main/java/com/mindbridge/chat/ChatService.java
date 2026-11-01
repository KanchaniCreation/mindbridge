package com.mindbridge.chat;

import com.mindbridge.common.ApiException;
import com.mindbridge.common.FixedWindowRateLimiter;
import com.mindbridge.common.PageResponse;
import com.mindbridge.user.User;
import com.mindbridge.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chat between two people who only see each other's pseudonyms.
 * Every method gets the user id from the session and checks membership, so a stranger's chat id
 * returns 404 exactly like an id that does not exist.
 */
@Service
public class ChatService {

    static final String FORMER_PARTICIPANT = "Former participant";
    private static final Duration INVITE_VALIDITY = Duration.ofHours(24);
    private static final int MAX_PENDING_INVITES = 5;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;
    private final ChatRepository chats;
    private final ChatParticipantRepository participants;
    private final ChatMessageRepository messages;
    private final ChatInviteRepository invites;
    private final ChatReportRepository reports;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final FixedWindowRateLimiter messageLimiter;
    private final FixedWindowRateLimiter redeemLimiter;

    public ChatService(UserRepository users, ChatRepository chats, ChatParticipantRepository participants,
                       ChatMessageRepository messages, ChatInviteRepository invites,
                       ChatReportRepository reports, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.chats = chats;
        this.participants = participants;
        this.messages = messages;
        this.invites = invites;
        this.reports = reports;
        this.events = events;
        this.clock = clock;
        this.messageLimiter = new FixedWindowRateLimiter(30, Duration.ofMinutes(1), clock);
        this.redeemLimiter = new FixedWindowRateLimiter(10, Duration.ofMinutes(15), clock);
    }

    @Transactional
    public void confirmAdult(UUID userId) {
        User user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
        user.confirmAdult(clock.instant());
    }

    @Transactional
    public InviteResponse createInvite(UUID userId) {
        requireAdult(userId);
        Instant now = clock.instant();
        if (invites.countByCreatorIdAndUsedAtIsNullAndExpiresAtAfter(userId, now) >= MAX_PENDING_INVITES) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "You already have 5 unused invites. Wait for one to be used or to expire.");
        }
        String code = InviteCodes.generate();
        Instant expiresAt = now.plus(INVITE_VALIDITY);
        invites.save(new ChatInvite(userId, InviteCodes.hash(code), expiresAt));
        return new InviteResponse(InviteCodes.format(code), expiresAt);
    }

    @Transactional
    public ChatSummaryResponse redeemInvite(UUID userId, String rawCode) {
        requireAdult(userId);
        if (!redeemLimiter.tryAcquire(userId.toString())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a few minutes.");
        }
        ChatInvite invite = invites.findByCodeHashForUpdate(InviteCodes.hash(InviteCodes.normalize(rawCode)))
            .orElseThrow(ChatService::inviteNotFound);
        Instant now = clock.instant();
        // Same generic answer for unknown, used, expired and own invites, so nothing is revealed.
        if (invite.getUsedAt() != null || !invite.getExpiresAt().isAfter(now)
                || invite.getCreatorId().equals(userId)) {
            throw inviteNotFound();
        }

        Chat chat = chats.save(new Chat());
        List<String> names = PseudonymGenerator.pair();
        participants.save(new ChatParticipant(chat.getId(), invite.getCreatorId(), names.get(0)));
        participants.save(new ChatParticipant(chat.getId(), userId, names.get(1)));
        invite.markUsed(now);

        return new ChatSummaryResponse(chat.getId(), chat.getStatus(), names.get(1), names.get(0),
            chat.getCreatedAt(), null);
    }

    @Transactional(readOnly = true)
    public List<ChatSummaryResponse> listChats(UUID userId) {
        List<ChatParticipant> mine = participants.findByUserId(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        List<UUID> chatIds = mine.stream().map(ChatParticipant::getChatId).toList();
        Map<UUID, Chat> chatById = chats.findAllById(chatIds).stream()
            .collect(Collectors.toMap(Chat::getId, c -> c));
        List<ChatParticipant> everyone = participants.findByChatIdIn(chatIds);

        return mine.stream()
            .filter(p -> chatById.containsKey(p.getChatId()))
            .map(p -> {
                Chat chat = chatById.get(p.getChatId());
                String partner = everyone.stream()
                    .filter(o -> o.getChatId().equals(p.getChatId()) && !o.getUserId().equals(userId))
                    .map(ChatParticipant::getPseudonym)
                    .findFirst()
                    .orElse(FORMER_PARTICIPANT);
                Instant lastMessageAt = messages.findFirstByChatIdOrderByCreatedAtDesc(chat.getId())
                    .map(ChatMessage::getCreatedAt).orElse(null);
                return new ChatSummaryResponse(chat.getId(), chat.getStatus(), p.getPseudonym(), partner,
                    chat.getCreatedAt(), lastMessageAt);
            })
            .sorted(Comparator.comparing(
                (ChatSummaryResponse c) -> c.lastMessageAt() != null ? c.lastMessageAt() : c.createdAt())
                .reversed())
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMessages(UUID userId, UUID chatId, int page, int size) {
        requireMember(userId, chatId);
        Map<UUID, String> names = pseudonymsOf(chatId);
        var result = messages.findByChatIdOrderByCreatedAtDesc(chatId,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
        return PageResponse.from(result.map(m -> toResponse(m, userId, names)));
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID userId, UUID chatId, MessageRequest request) {
        requireAdult(userId);
        requireMember(userId, chatId);
        Chat chat = chats.findById(chatId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found."));
        Map<UUID, String> names = pseudonymsOf(chatId);

        // The same clientMessageId again (a retry) returns the stored message and creates nothing.
        Optional<ChatMessage> existing = messages
            .findByChatIdAndSenderIdAndClientMessageId(chatId, userId, request.clientMessageId());
        if (existing.isPresent()) {
            return toResponse(existing.get(), userId, names);
        }
        if (chat.getStatus() != ChatStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "This chat is closed.");
        }
        if (!messageLimiter.tryAcquire(userId.toString())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "You are sending messages too quickly. Please slow down.");
        }

        ChatMessage saved;
        try {
            saved = messages.saveAndFlush(
                new ChatMessage(chatId, userId, request.clientMessageId(), request.body().trim()));
        } catch (DataIntegrityViolationException ex) {
            // Two identical requests raced; the unique constraint let only one through.
            throw new ApiException(HttpStatus.CONFLICT, "Duplicate request. Please try again.");
        }
        events.publishEvent(new ChatMessageSent(chatId, saved.getId(), userId, saved.getBody(),
            saved.getCreatedAt(), names));
        return toResponse(saved, userId, names);
    }

    /** Either person can close a chat at any time. Closing is how "block" works. */
    @Transactional
    public void close(UUID userId, UUID chatId) {
        requireMember(userId, chatId);
        closeChat(userId, chatId);
    }

    /** Reporting stores the reason only (no message text) and closes the chat. */
    @Transactional
    public void report(UUID userId, UUID chatId, ReportRequest request) {
        requireMember(userId, chatId);
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        reports.save(new ChatReport(chatId, userId, request.reason(), note));
        closeChat(userId, chatId);
    }

    @Transactional(readOnly = true)
    public void requireMembership(UUID userId, UUID chatId) {
        requireMember(userId, chatId);
    }

    private void closeChat(UUID userId, UUID chatId) {
        Chat chat = chats.findById(chatId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found."));
        if (chat.getStatus() == ChatStatus.ACTIVE) {
            chat.close(userId, clock.instant());
            events.publishEvent(new ChatClosed(chatId));
        }
    }

    private ChatParticipant requireMember(UUID userId, UUID chatId) {
        return participants.findByChatIdAndUserId(chatId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found."));
    }

    private void requireAdult(UUID userId) {
        boolean confirmed = users.findById(userId)
            .map(u -> u.getAdultConfirmedAt() != null)
            .orElse(false);
        if (!confirmed) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                "Please confirm that you are 18 or older to use chat.");
        }
    }

    private Map<UUID, String> pseudonymsOf(UUID chatId) {
        Map<UUID, String> names = new HashMap<>();
        for (ChatParticipant p : participants.findByChatId(chatId)) {
            names.put(p.getUserId(), p.getPseudonym());
        }
        return names;
    }

    private static ChatMessageResponse toResponse(ChatMessage m, UUID viewerId, Map<UUID, String> names) {
        return new ChatMessageResponse(m.getId(), m.getSenderId().equals(viewerId),
            names.getOrDefault(m.getSenderId(), FORMER_PARTICIPANT), m.getBody(), m.getCreatedAt());
    }

    private static ApiException inviteNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "That invite code is not valid or has expired.");
    }
}
