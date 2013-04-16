package ru.taskurotta.backend.config.impl;

import java.util.List;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

public class MemoryConfigBackend implements ConfigBackend {

	private ActorPreferences[] actorPreferences;
	private ExpirationPolicyConfig[] expirationPolicies;

	@Override
	public boolean isActorBlocked(String actorId) {
		return false;
	}

	@Override
	public ActorPreferences[] getActorPreferences() {
		return actorPreferences;
	}

	public void setActorPreferences(ActorPreferences[] actorPreferences) {
		this.actorPreferences = actorPreferences;
	}

	@Override
    public ExpirationPolicyConfig[] getExpirationPolicies() {
        return expirationPolicies;
    }

    public void setExpirationPolicies(ExpirationPolicyConfig[] expirationPolicies) {
        this.expirationPolicies = expirationPolicies;
    }

}
