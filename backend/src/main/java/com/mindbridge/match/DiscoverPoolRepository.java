package com.mindbridge.match;

import com.mindbridge.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Finds people a given user may meet. A person is left out when they are the user, not open to
 * chat, not an adult-confirmed user with an alias, already answered by the user, someone who
 * skipped the user, or someone the user already has (or had) a chat with. A closed or reported
 * chat therefore keeps the two apart for good.
 */
@Repository
public class DiscoverPoolRepository {

    private static final String ELIGIBLE = """
        u.id <> :me
        AND u.open_to_chat = TRUE
        AND u.alias IS NOT NULL
        AND u.adult_confirmed_at IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM match_decisions d
                        WHERE d.actor_id = :me AND d.target_id = u.id)
        AND NOT EXISTS (SELECT 1 FROM match_decisions d
                        WHERE d.actor_id = u.id AND d.target_id = :me AND d.decision = 'SKIP')
        AND NOT EXISTS (SELECT 1 FROM chat_participants mine
                        JOIN chat_participants theirs ON theirs.chat_id = mine.chat_id
                        WHERE mine.user_id = :me AND theirs.user_id = u.id)
        """;

    @PersistenceContext
    private EntityManager em;

    /** A random sample, so the same people are not always first. Ranking happens afterwards. */
    @SuppressWarnings("unchecked")
    public List<User> findPool(UUID me, int limit) {
        return em.createNativeQuery(
                "SELECT u.* FROM users u WHERE " + ELIGIBLE + " ORDER BY random() LIMIT :limit", User.class)
            .setParameter("me", me)
            .setParameter("limit", limit)
            .getResultList();
    }

    public boolean isAvailable(UUID me, UUID target) {
        Number count = (Number) em.createNativeQuery(
                "SELECT count(*) FROM users u WHERE " + ELIGIBLE + " AND u.id = :target")
            .setParameter("me", me)
            .setParameter("target", target)
            .getSingleResult();
        return count.longValue() > 0;
    }
}
