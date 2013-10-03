package ru.taskurotta.backend.config.impl;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.ConfigBackendUtils;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Configuration storing
 */
public class MemoryConfigBackend implements ConfigBackend {

	private Collection<ActorPreferences> actorPreferences;
	private Collection<ExpirationPolicyConfig> expirationPolicies;

	private int defaultTimeout = 1;
	private TimeUnit defaultTimeunit  = TimeUnit.SECONDS;

	public boolean isActorBlocked(String actorId) {
        boolean result = false;
        Collection<ActorPreferences> actorPreferences = getAllActorPreferences();
        if (actorPreferences!=null && !actorPreferences.isEmpty()) {
            for(ActorPreferences aPref: actorPreferences) {
                if (aPref.getId().equals(actorId)) {
                    result = aPref.isBlocked();
                    break;
                }
            }
        }
        return result;

	}

    @Override
    public void blockActor(String actorId) {
        setBlockedState(actorId, true);
    }

    private void setBlockedState(String actorId, boolean isBlocked) {
        Collection<ActorPreferences> actorPreferences = getAllActorPreferences();
        boolean checked = false;
        if (actorPreferences!=null && !actorPreferences.isEmpty()) {
            for(ActorPreferences aPref: actorPreferences) {
                if (aPref.getId().equals(actorId)) {
                    aPref.setBlocked(isBlocked);
                    checked = true;
                    break;
                }
            }

            if (!checked) {
                ActorPreferences ap = new ActorPreferences();
                ap.setId(actorId);
                ap.setBlocked(isBlocked);
                actorPreferences.add(ap);
            }

        }
    }

    @Override
    public void unblockActor(String actorId) {
        setBlockedState(actorId, false);
    }

	public Collection<ActorPreferences> getAllActorPreferences() {
	    if (actorPreferences == null) {
	        actorPreferences = ConfigBackendUtils.getDefaultActorPreferences();
	    }
		return actorPreferences;
	}

	public void setActorPreferences(ActorPreferences[] actorPreferences) {
        if (actorPreferences!=null) {
            this.actorPreferences = Arrays.asList(actorPreferences);
        } else {
            this.actorPreferences = null;
        }
	}

    public void setActorPreferencesCollection(Collection<ActorPreferences> actorPreferences) {
        this.actorPreferences = actorPreferences;
    }

    public Collection<ExpirationPolicyConfig> getAllExpirationPolicies() {
	    if (expirationPolicies == null) {
	        expirationPolicies = ConfigBackendUtils.getDefaultPolicies(defaultTimeout, defaultTimeunit);
	    }
        return expirationPolicies;
    }

    public ActorPreferences getActorPreferences(String actorId) {
        ActorPreferences result = null;
        if (actorPreferences!=null) {
            for(ActorPreferences ap: actorPreferences) {
                if (ap.getId()!=null && ap.getId().equals(actorId)) {
                    result = ap;
                    break;
                }
            }
        }
        return result;
    }

    public void setExpirationPolicies(ExpirationPolicyConfig[] expirationPolicies) {
        if (expirationPolicies!=null) {
            this.expirationPolicies = Arrays.asList(expirationPolicies);
        } else {
            this.expirationPolicies = null;
        }

    }

    public void setExpirationPoliciesCollection(Collection<ExpirationPolicyConfig> expirationPolicies) {
        this.expirationPolicies = expirationPolicies;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public void setDefaultTimeunit(TimeUnit defaultTimeunit) {
        this.defaultTimeunit = defaultTimeunit;
    }

}
