package ru.taskurotta.backend.hz.config;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 17.06.13
 * Time: 15:35
 */
public class HzConfigBackend implements ConfigBackend {

    private static final Logger logger = LoggerFactory.getLogger(HzConfigBackend.class);

    private HazelcastInstance hazelcastInstance;

    public static final String ACTOR_PREFERENCES_MAP_NAME = "actorPreferencesMap";
    public static final String EXPIRATION_POLICY_CONFIG_SET_NAME = "expirationPolicyConfigSet";

    private Map<String, ActorPreferences> actorPreferencesMap = new HashMap<>();
    private Set<ExpirationPolicyConfig> expirationPolicyConfigSet = new HashSet<>();

    private int defaultTimeout = 1;
    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    public HzConfigBackend(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;

        init();
    }

    public HzConfigBackend(HazelcastInstance hazelcastInstance, int defaultTimeout, TimeUnit defaultTimeUnit) {
        this.hazelcastInstance = hazelcastInstance;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;

        init();
    }

    private void init() {
        IMap<String, ActorPreferences> iMap = hazelcastInstance.getMap(ACTOR_PREFERENCES_MAP_NAME);
        actorPreferencesMap.putAll(iMap);

        iMap.addEntryListener(new EntryListener<String, ActorPreferences>() {
            @Override
            public void entryAdded(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                logger.trace("Add to [{}] item [{}]", ACTOR_PREFERENCES_MAP_NAME, stringActorPreferencesEntryEvent);
                actorPreferencesMap.put(stringActorPreferencesEntryEvent.getKey(), stringActorPreferencesEntryEvent.getValue());
            }

            @Override
            public void entryRemoved(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                logger.trace("Remove from [{}] item [{}]", ACTOR_PREFERENCES_MAP_NAME, stringActorPreferencesEntryEvent);
                actorPreferencesMap.remove(stringActorPreferencesEntryEvent.getKey());
            }

            @Override
            public void entryUpdated(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                logger.trace("Update [{}] by item [{}]", ACTOR_PREFERENCES_MAP_NAME, stringActorPreferencesEntryEvent);
                actorPreferencesMap.put(stringActorPreferencesEntryEvent.getKey(), stringActorPreferencesEntryEvent.getValue());
            }

            @Override
            public void entryEvicted(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                logger.trace("Evict from [{}] item [{}]", ACTOR_PREFERENCES_MAP_NAME, stringActorPreferencesEntryEvent);
                actorPreferencesMap.remove(stringActorPreferencesEntryEvent.getKey());
            }
        }, true);


        ISet<ExpirationPolicyConfig> iSet = hazelcastInstance.getSet(EXPIRATION_POLICY_CONFIG_SET_NAME);
        expirationPolicyConfigSet.addAll(iSet);

        iSet.addItemListener(new ItemListener<ExpirationPolicyConfig>() {
            @Override
            public void itemAdded(ItemEvent<ExpirationPolicyConfig> expirationPolicyConfigItemEvent) {
                logger.trace("Add to [{}] item [{}]", EXPIRATION_POLICY_CONFIG_SET_NAME, expirationPolicyConfigItemEvent);
                expirationPolicyConfigSet.add(expirationPolicyConfigItemEvent.getItem());
            }

            @Override
            public void itemRemoved(ItemEvent<ExpirationPolicyConfig> expirationPolicyConfigItemEvent) {
                logger.trace("Remove from [{}] item [{}]", EXPIRATION_POLICY_CONFIG_SET_NAME, expirationPolicyConfigItemEvent);
                expirationPolicyConfigSet.remove(expirationPolicyConfigItemEvent.getItem());
            }
        }, false);
    }

    @Override
    public boolean isActorBlocked(String actorId) {
        if (!actorPreferencesMap.containsKey(actorId)) {
            return false;
        }

        ActorPreferences actorPreferences = actorPreferencesMap.get(actorId);
        return actorPreferences.isBlocked();
    }

    @Override
    public ActorPreferences[] getAllActorPreferences() {
        if (actorPreferencesMap.isEmpty()) {
            return getDefaultActorPreferences();
        }

        return (ActorPreferences[]) actorPreferencesMap.values().toArray();
    }

    @Override
    public ExpirationPolicyConfig[] getAllExpirationPolicies() {
        if (expirationPolicyConfigSet.isEmpty()) {
            return getDefaultPolicies(defaultTimeout, defaultTimeUnit);
        }

        return (ExpirationPolicyConfig[]) expirationPolicyConfigSet.toArray();
    }

    @Override
    public ActorPreferences getActorPreferences(String actorId) {
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
