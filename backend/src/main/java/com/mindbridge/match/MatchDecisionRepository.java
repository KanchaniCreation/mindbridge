package com.mindbridge.match;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchDecisionRepository extends JpaRepository<MatchDecision, UUID> {

    boolean existsByActorIdAndTargetId(UUID actorId, UUID targetId);

    boolean existsByActorIdAndTargetIdAndDecision(UUID actorId, UUID targetId, Decision decision);
}
