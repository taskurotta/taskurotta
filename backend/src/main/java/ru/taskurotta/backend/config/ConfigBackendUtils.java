package ru.taskurotta.backend.config;

import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * User: dimadin
 * Date: 03.10.13 18:08
 */
public class ConfigBackendUtils {

    public static Collection<ActorPreferences> getDefaultActorPreferences() {
        Collection<ActorPreferences> result = new ArrayList();
        ActorPreferences defaultActorPrefs = new ActorPreferences();
        defaultActorPrefs.setBlocked(false);
        defaultActorPrefs.setId("default");
        result.add(defaultActorPrefs);
        return result;
    }

    public static Collection<ExpirationPolicyConfig> getDefaultPolicies(Integer timeout, TimeUnit unit) {
        Collection<ExpirationPolicyConfig> result = new ArrayList();
        ExpirationPolicyConfig timeoutPolicy = new ExpirationPolicyConfig();
        timeoutPolicy.setName("default_timeout_policy");
        timeoutPolicy.setClassName("ru.taskurotta.server.config.expiration.impl.TimeoutPolicy");
        Properties policyProps = new Properties();
        policyProps.put("timeout", timeout);
        policyProps.put("timeUnit", unit.toString());
        timeoutPolicy.setProperties(policyProps);
        result.add(timeoutPolicy);
        return result;
    }

}
