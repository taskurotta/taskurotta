package ru.taskurotta.service.config;

import ru.taskurotta.service.config.model.ActorPreferences;
import ru.taskurotta.service.config.model.ExpirationPolicyConfig;

import java.util.Collection;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:08 PM
 */
public interface ConfigService {

    boolean isActorBlocked(String actorId);

    void blockActor(String actorId);

    void unblockActor(String actorId);

    Collection<ActorPreferences> getAllActorPreferences();

    Collection<ExpirationPolicyConfig> getAllExpirationPolicies();

    ActorPreferences getActorPreferences(String actorId);

}
