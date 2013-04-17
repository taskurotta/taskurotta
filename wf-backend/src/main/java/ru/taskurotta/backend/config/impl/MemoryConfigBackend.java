package ru.taskurotta.backend.config.impl;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

public class MemoryConfigBackend implements ConfigBackend {

	private ActorPreferences[] actorPreferences;
	private ExpirationPolicyConfig[] expirationPolicies;

	private int defaultTimeout = 4;
	private TimeUnit defaultTimeunit  = TimeUnit.SECONDS;
	private boolean fallBackToDefaults = true;

	@Override
	public boolean isActorBlocked(String actorId) {
		return false;
	}

	private ActorPreferences[] getDefaultActorPreferences() {
	    ActorPreferences[] result = new ActorPreferences[1];
	    ActorPreferences defaultActorPrefs = new ActorPreferences();
	    defaultActorPrefs.setBlocked(false);
	    defaultActorPrefs.setId("default");
	    defaultActorPrefs.setType("default");
	    Properties expirationPolicies = new Properties();
	    expirationPolicies.put("PROCESS_SCHEDULE_TO_START", "default_timeout_policy");
	    expirationPolicies.put("PROCESS_SCHEDULE_TO_CLOSE", "default_timeout_policy");
	    expirationPolicies.put("TASK_POLL_TO_COMMIT", "default_timeout_policy");
	    expirationPolicies.put("TASK_RELEASE_TO_COMMIT", "default_timeout_policy");
	    expirationPolicies.put("TASK_SCHEDULE_TO_CLOSE", "default_timeout_policy");
	    expirationPolicies.put("TASK_SCHEDULE_TO_START", "default_timeout_policy");
	    expirationPolicies.put("TASK_START_TO_CLOSE", "default_timeout_policy");
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
	public ActorPreferences[] getActorPreferences() {
	    if(actorPreferences == null && fallBackToDefaults) {
	        actorPreferences = getDefaultActorPreferences();
	    }
		return actorPreferences;
	}

	public void setActorPreferences(ActorPreferences[] actorPreferences) {
		this.actorPreferences = actorPreferences;
	}

	@Override
    public ExpirationPolicyConfig[] getExpirationPolicies() {
	    if(expirationPolicies == null && fallBackToDefaults) {
	        expirationPolicies = getDefaultPolicies(defaultTimeout, defaultTimeunit);
	    }
        return expirationPolicies;
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

    public void setFallBackToDefaults(boolean fallBackToDefaults) {
        this.fallBackToDefaults = fallBackToDefaults;
    }

}
