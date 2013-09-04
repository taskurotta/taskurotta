package ru.taskurotta.backend.config.impl;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Configuration storing
 */
public class MemoryConfigBackend implements ConfigBackend {

	private ActorPreferences[] actorPreferences;
	private ExpirationPolicyConfig[] expirationPolicies;

	private int defaultTimeout = 1;
	private TimeUnit defaultTimeunit  = TimeUnit.SECONDS;
	
	@Override
	public boolean isActorBlocked(String actorId) {
        boolean result = false;
        ActorPreferences[] actorPreferences = getAllActorPreferences();
        if (actorPreferences!=null && actorPreferences.length>0) {
            for(ActorPreferences aPref: actorPreferences) {
                if (aPref.getId().equals(actorId)) {
                    result = aPref.isBlocked();
                    break;
                }
            }
        }
        return result;

	}

	private ActorPreferences[] getDefaultActorPreferences() {
	    ActorPreferences[] result = new ActorPreferences[1];
	    ActorPreferences defaultActorPrefs = new ActorPreferences();
	    defaultActorPrefs.setBlocked(false);
	    defaultActorPrefs.setId("default");
	    Properties expirationPolicies = new Properties();
	    for(TimeoutType timeoutType: TimeoutType.values()) {
	        expirationPolicies.put(timeoutType.toString(), "default_timeout_policy");
	    }
	    defaultActorPrefs.setTimeoutPolicies(expirationPolicies);
	    result[0] = defaultActorPrefs;
	    return result;
	}

	private ExpirationPolicyConfig[] getDefaultPolicies(Integer timeout, TimeUnit unit) {
	    ExpirationPolicyConfig[] result = new ExpirationPolicyConfig[1];
	    ExpirationPolicyConfig timeoutPolicy = new ExpirationPolicyConfig();
	    timeoutPolicy.setName("default_timeout_policy");
	    timeoutPolicy.setClassName("ru.taskurotta.server.config.expiration.impl.TimeoutPolicy");
	    Properties policyProps = new Properties();
	    policyProps.put("timeout", timeout);
	    policyProps.put("timeUnit", unit.toString());
	    timeoutPolicy.setProperties(policyProps);
	    result[0] = timeoutPolicy;
	    return result;
	}

	@Override
	public ActorPreferences[] getAllActorPreferences() {
	    if (actorPreferences == null) {
	        actorPreferences = getDefaultActorPreferences();
	    }
		return actorPreferences;
	}

	public void setActorPreferences(ActorPreferences[] actorPreferences) {
		this.actorPreferences = actorPreferences;
	}

	@Override
    public ExpirationPolicyConfig[] getAllExpirationPolicies() {
	    if (expirationPolicies == null) {
	        expirationPolicies = getDefaultPolicies(defaultTimeout, defaultTimeunit);
	    }
        return expirationPolicies;
    }

    @Override
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
        this.expirationPolicies = expirationPolicies;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public void setDefaultTimeunit(TimeUnit defaultTimeunit) {
        this.defaultTimeunit = defaultTimeunit;
    }

}
