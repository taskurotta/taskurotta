package ru.taskurotta.backend.config;

import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.Collection;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:08 PM
 */
public interface ConfigBackend {

    public boolean isActorBlocked(String actorId);

    public void blockActor(String actorId);

    public void unblockActor(String actorId);

    public Collection<ActorPreferences> getAllActorPreferences();

    public Collection<ExpirationPolicyConfig> getAllExpirationPolicies();

    public ActorPreferences getActorPreferences(String actorId);

}
