package ru.taskurotta.backend.hz.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 17.06.13
 * Time: 15:35
 */
public class HzConfigBackend implements ConfigBackend {

    private HazelcastInstance hazelcastInstance;

    public static final String ACTOR_PREFERENCES_MAP_NAME = "actorPreferencesMap";
    public static final String EXPIRATION_POLICY_CONFIG_SET_NAME = "expirationPolicyConfigSet";

    private int defaultTimeout = 1;
    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    public HzConfigBackend(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public HzConfigBackend(HazelcastInstance hazelcastInstance, int defaultTimeout, TimeUnit defaultTimeUnit) {
        this.hazelcastInstance = hazelcastInstance;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    @Override
    public boolean isActorBlocked(String actorId) {
        IMap<String, ActorPreferences> actorPreferencesMap = hazelcastInstance.getMap(ACTOR_PREFERENCES_MAP_NAME);

        if (!actorPreferencesMap.containsKey(actorId)) {
            return false;
        }

        ActorPreferences actorPreferences = actorPreferencesMap.get(actorId);
        return actorPreferences.isBlocked();
    }

    @Override
    public ActorPreferences[] getAllActorPreferences() {
        IMap<String, ActorPreferences> actorPreferencesMap = hazelcastInstance.getMap(ACTOR_PREFERENCES_MAP_NAME);

        if (actorPreferencesMap.isEmpty()) {
            return getDefaultActorPreferences();
        }

        return (ActorPreferences[]) actorPreferencesMap.values().toArray();
    }

    @Override
    public ExpirationPolicyConfig[] getAllExpirationPolicies() {
        ISet<ExpirationPolicyConfig> expirationPolicyConfigSet = hazelcastInstance.getSet(EXPIRATION_POLICY_CONFIG_SET_NAME);

        if (expirationPolicyConfigSet.isEmpty()) {
            return getDefaultPolicies(defaultTimeout, defaultTimeUnit);
        }

        return (ExpirationPolicyConfig[]) expirationPolicyConfigSet.toArray();
    }

    @Override
    public ActorPreferences getActorPreferences(String actorId) {
        IMap<String, ActorPreferences> actorPreferencesMap = hazelcastInstance.getMap(ACTOR_PREFERENCES_MAP_NAME);

        if (!actorPreferencesMap.containsKey(actorId)) {
            return null;
        }

        return actorPreferencesMap.get(actorId);
    }

    private ActorPreferences[] getDefaultActorPreferences() {
        ActorPreferences actorPreferences = new ActorPreferences();
        actorPreferences.setBlocked(false);
        actorPreferences.setId("default");

        Properties expirationPolicies = new Properties();
        for(TimeoutType timeoutType: TimeoutType.values()) {
            expirationPolicies.put(timeoutType.toString(), "default_timeout_policy");
        }
        actorPreferences.setTimeoutPolicies(expirationPolicies);

        return new ActorPreferences[] {actorPreferences};
    }

    private ExpirationPolicyConfig[] getDefaultPolicies(Integer timeout, TimeUnit unit) {
        ExpirationPolicyConfig timeoutPolicy = new ExpirationPolicyConfig();
        timeoutPolicy.setName("default_timeout_policy");
        timeoutPolicy.setClassName("ru.taskurotta.server.config.expiration.impl.TimeoutPolicy");

        Properties policyProps = new Properties();
        policyProps.put("timeout", timeout);
        policyProps.put("timeUnit", unit.toString());
        timeoutPolicy.setProperties(policyProps);

        return new ExpirationPolicyConfig[] {timeoutPolicy};
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public TimeUnit getDefaultTimeUnit() {
        return defaultTimeUnit;
    }

    public void setDefaultTimeUnit(TimeUnit defaultTimeUnit) {
        this.defaultTimeUnit = defaultTimeUnit;
    }
}
