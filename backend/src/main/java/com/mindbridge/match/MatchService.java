package com.mindbridge.match;

import com.mindbridge.chat.AdultGate;
import com.mindbridge.chat.Chat;
import com.mindbridge.chat.ChatParticipant;
import com.mindbridge.chat.ChatParticipantRepository;
import com.mindbridge.chat.ChatRepository;
import com.mindbridge.chat.ChatSummaryResponse;
import com.mindbridge.common.ApiException;
import com.mindbridge.common.FixedWindowRateLimiter;
import com.mindbridge.user.User;
import com.mindbridge.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two people meet only when both choose Match. One-sided Matches stay private, a Skip hides the
 * pair for good, and the user id always comes from the session.
 */
@Service
public class MatchService {

    static final int DISCOVER_MAX = 20;
    static final int POOL_SIZE = 200;
    static final int DAILY_ANSWER_LIMIT = 20;
    static final int MAX_OPEN_CHATS = 5;

    /** Names that could be mistaken for staff or the app itself. */
    private static final Set<String> RESERVED_ALIASES = Set.of(
        "admin", "administrator", "moderator", "support", "mindbridge", "staff", "system", "official");

    private final UserRepository users;
    private final MatchDecisionRepository decisions;
    private final DiscoverPoolRepository pool;
    private final ChatRepository chats;
    private final ChatParticipantRepository participants;
    private final AdultGate adultGate;
    private final JdbcTemplate jdbc;
    private final FixedWindowRateLimiter answerLimiter;

    public MatchService(UserRepository users, MatchDecisionRepository decisions, DiscoverPoolRepository pool,
                        ChatRepository chats, ChatParticipantRepository participants, AdultGate adultGate,
                        JdbcTemplate jdbc, Clock clock) {
        this.users = users;
        this.decisions = decisions;
        this.pool = pool;
        this.chats = chats;
        this.participants = participants;
        this.adultGate = adultGate;
        this.jdbc = jdbc;
        this.answerLimiter = new FixedWindowRateLimiter(DAILY_ANSWER_LIMIT, Duration.ofHours(24), clock);
    }

    @Transactional(readOnly = true)
    public MatchProfileResponse getProfile(UUID userId) {
        return MatchProfileResponse.from(findUser(userId));
    }

    @Transactional
    public MatchProfileResponse updateProfile(UUID userId, MatchProfileRequest request) {
        User me = findUser(userId);
        String alias = request.alias().trim();
        if (RESERVED_ALIASES.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That alias is not available.");
        }
        if (users.existsByAliasIgnoreCaseAndIdNot(alias, userId)) {
            throw aliasTaken();
        }
        Set<Topic> topics = EnumSet.noneOf(Topic.class);
        if (request.topics() != null) {
            topics.addAll(request.topics());
        }
        if (request.openToChat() && topics.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose at least one topic to be open to chat.");
        }
        me.updateMatchProfile(alias, clean(request.about()), clean(request.realName()),
            Topic.join(topics), request.openToChat());
        try {
            // Flush now so a race on the unique alias fails here and not later.
            users.saveAndFlush(me);
        } catch (DataIntegrityViolationException ex) {
            throw aliasTaken();
        }
        return MatchProfileResponse.from(me);
    }

    @Transactional(readOnly = true)
    public List<DiscoverCard> discover(UUID userId, int size) {
        User me = requireReady(userId);
        int limit = Math.min(Math.max(size, 1), DISCOVER_MAX);
        Set<Topic> mine = Topic.parse(me.getTopicsCsv());
        return MatchRanker.rank(mine, pool.findPool(userId, POOL_SIZE), limit).stream()
            .map(r -> new DiscoverCard(r.user().getAlias(), r.user().getAbout(),
                List.copyOf(r.topics()), List.copyOf(r.shared())))
            .toList();
    }

    @Transactional
    public MatchResult decide(UUID userId, DecisionRequest request) {
        User me = requireReady(userId);
        // Counted before looking anyone up, so guessing aliases is limited too.
        if (!answerLimiter.tryAcquire(userId.toString())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "You have reached today's limit of " + DAILY_ANSWER_LIMIT + " answers. Come back tomorrow.");
        }
        User target = users.findByAliasIgnoreCase(request.alias().trim())
            .filter(t -> !t.getId().equals(userId))
            .orElseThrow(MatchService::notAvailable);

        // Both people answering at the same moment must not miss each other's Match.
        lockPair(userId, target.getId());

        if (decisions.existsByActorIdAndTargetId(userId, target.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already answered for this person.");
        }
        if (!pool.isAvailable(userId, target.getId())) {
            throw notAvailable();
        }
        if (request.decision() == Decision.MATCH && chats.countActiveChatsOf(userId) >= MAX_OPEN_CHATS) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have " + MAX_OPEN_CHATS
                + " open chats. Close one before matching with someone new.");
        }
        try {
            decisions.saveAndFlush(new MatchDecision(userId, target.getId(), request.decision()));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already answered for this person.");
        }

        boolean mutual = request.decision() == Decision.MATCH
            && decisions.existsByActorIdAndTargetIdAndDecision(target.getId(), userId, Decision.MATCH);
        if (!mutual) {
            return MatchResult.none();
        }

        Chat chat = chats.save(new Chat());
        participants.save(new ChatParticipant(chat.getId(), me.getId(), me.getAlias()));
        participants.save(new ChatParticipant(chat.getId(), target.getId(), target.getAlias()));
        return new MatchResult(true, new ChatSummaryResponse(chat.getId(), chat.getStatus(),
            me.getAlias(), target.getAlias(), chat.getCreatedAt(), null));
    }

    /** Adult confirmed, alias chosen and open to chat: the three things needed to meet people. */
    private User requireReady(UUID userId) {
        adultGate.requireAdult(userId);
        User me = findUser(userId);
        if (me.getAlias() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Choose an alias before you start matching.");
        }
        if (!me.isOpenToChat()) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Turn on \"open to chat\" in your profile to meet people.");
        }
        return me;
    }

    private void lockPair(UUID a, UUID b) {
        String key = a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
        jdbc.queryForObject(
            "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(?, 0))) AS l", Integer.class, key);
    }

    private User findUser(UUID userId) {
        return users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException aliasTaken() {
        return new ApiException(HttpStatus.CONFLICT, "That alias is already taken.");
    }

    /** One answer for unknown, closed, self, and hidden people, so nothing is revealed. */
    private static ApiException notAvailable() {
        return new ApiException(HttpStatus.NOT_FOUND, "That person is not available.");
    }
}
