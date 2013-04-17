package ru.taskurotta.dropwizard.internal;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;

public class YamlConfigBackend implements ConfigBackend, ConfigBackendAware {

    private ConfigBackend config;

    @Override
    public boolean isActorBlocked(String actorId) {
        boolean result = false;
        ActorPreferences[] actorPreferences = getActorPreferences();
        if(actorPreferences!=null && actorPreferences.length>0) {
            for(ActorPreferences aPref: actorPreferences) {
                if(aPref.getId().equals(actorId)) {
                    result = aPref.isBlocked();
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public ActorPreferences[] getActorPreferences() {
        return config!=null? config.getActorPreferences(): null;
    }

    @Override
    public void setConfigBackend(ConfigBackend config) {
        this.config = config;
    }

    public ExpirationPolicyConfig[] getExpirationPolicies() {
        return config.getExpirationPolicies();
    }

}
