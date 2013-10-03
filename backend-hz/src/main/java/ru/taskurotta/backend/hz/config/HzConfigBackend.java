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
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.ConfigBackendUtils;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;
import ru.taskurotta.backend.console.retriever.ConfigInfoRetriever;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 17.06.13
 * Time: 15:35
 */
public class HzConfigBackend implements ConfigBackend, ConfigInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HzConfigBackend.class);

    private HazelcastInstance hazelcastInstance;

    public static final String EXPIRATION_POLICY_CONFIG_SET_NAME = "expirationPolicyConfigSet";

    private volatile Map<String, ActorPreferences> localActorPreferences = new HashMap<>();
    private volatile Set<ExpirationPolicyConfig> localExpPolicies = new HashSet<>();

    private int defaultTimeout = 1;
    private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    private String actorPreferencesMapName;

    public HzConfigBackend(HazelcastInstance hazelcastInstance, String actorPreferencesMapName) {
        this.hazelcastInstance = hazelcastInstance;
        this.actorPreferencesMapName = actorPreferencesMapName;

        init();
    }

    public HzConfigBackend(HazelcastInstance hazelcastInstance, int defaultTimeout, TimeUnit defaultTimeUnit, String actorPreferencesMapName) {
        this.hazelcastInstance = hazelcastInstance;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;
        this.actorPreferencesMapName = actorPreferencesMapName;

        init();
    }

    private void init() {
        IMap<String, ActorPreferences> distributedActorPreferences = hazelcastInstance.getMap(actorPreferencesMapName);

        distributedActorPreferences.addEntryListener(new EntryListener<String, ActorPreferences>() {
            @Override
            public void entryAdded(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                updateActorPreferencesMap();
            }

            @Override
            public void entryRemoved(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                updateActorPreferencesMap();
            }

            @Override
            public void entryUpdated(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                updateActorPreferencesMap();
            }

            @Override
            public void entryEvicted(EntryEvent<String, ActorPreferences> stringActorPreferencesEntryEvent) {
                updateActorPreferencesMap();
            }
        }, false);

        ISet<ExpirationPolicyConfig> distributedExpPolicies = hazelcastInstance.getSet(EXPIRATION_POLICY_CONFIG_SET_NAME);

        distributedExpPolicies.addItemListener(new ItemListener<ExpirationPolicyConfig>() {
            @Override
            public void itemAdded(ItemEvent<ExpirationPolicyConfig> expirationPolicyConfigItemEvent) {
                updateExpirationPolicyConfigSet();
            }

            @Override
            public void itemRemoved(ItemEvent<ExpirationPolicyConfig> expirationPolicyConfigItemEvent) {
                updateExpirationPolicyConfigSet();
            }
        }, false);

        updateActorPreferencesMap();
        updateExpirationPolicyConfigSet();
    }

    @Override
    public boolean isActorBlocked(String actorId) {
        ActorPreferences actorPreferences = localActorPreferences.get(actorId);
        logger.debug("actorPreferences getted in isActorBlocked[{]] are [{}] ", actorId, actorPreferences);
        return actorPreferences != null && actorPreferences.isBlocked();
    }

    @Override
    public void blockActor(String actorId) {
        setBlockedState(actorId, true);
    }

    private void setBlockedState(String actorId, boolean isBlocked) {
        IMap<String, ActorPreferences> distributedActorPreferences = hazelcastInstance.getMap(actorPreferencesMapName);
        ActorPreferences value = null;

        if (!distributedActorPreferences.containsKey(actorId)) {
            value = new ActorPreferences();
            value.setId(actorId);
        } else {
            value = distributedActorPreferences.get(actorId);
        }

        value.setBlocked(isBlocked);
        distributedActorPreferences.put(actorId, value);

    }

    @Override
    public void unblockActor(String actorId) {
        setBlockedState(actorId, false);
    }

    public Collection<ActorPreferences> getAllActorPreferences() {
        if (localActorPreferences.isEmpty()) {
            for (ActorPreferences ap: ConfigBackendUtils.getDefaultActorPreferences()) {
                localActorPreferences.put(ap.getId(), ap);
            }
        }

        return localActorPreferences.values();
    }

    public Collection<ExpirationPolicyConfig> getAllExpirationPolicies() {
        if (localExpPolicies.isEmpty()) {
            localExpPolicies.addAll(ConfigBackendUtils.getDefaultPolicies(defaultTimeout, defaultTimeUnit));
        }

        return localExpPolicies;
    }

    public ActorPreferences getActorPreferences(String actorId) {
        if (!localActorPreferences.containsKey(actorId)) {
            return null;
        }

        return localActorPreferences.get(actorId);
    }

    private void updateActorPreferencesMap() {
        IMap<String, ActorPreferences> distributedActorPreferences = hazelcastInstance.getMap(actorPreferencesMapName);
        logger.trace("Update [{}] = [{}]", actorPreferencesMapName, distributedActorPreferences);

        // to performance reason
        localActorPreferences = new HashMap<>(distributedActorPreferences);
    }

    private void updateExpirationPolicyConfigSet() {
        ISet<ExpirationPolicyConfig> distributedExpPolicies = hazelcastInstance.getSet(EXPIRATION_POLICY_CONFIG_SET_NAME);
        logger.trace("Update [{}] = [{}]", EXPIRATION_POLICY_CONFIG_SET_NAME, distributedExpPolicies);

        // for is blocked check performance reason
        localExpPolicies = new HashSet<>(distributedExpPolicies);
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
