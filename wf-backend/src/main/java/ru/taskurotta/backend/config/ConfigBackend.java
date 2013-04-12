package ru.taskurotta.backend.config;

import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:08 PM
 */
public interface ConfigBackend {
	
    public boolean isActorBlocked(ActorDefinition actorDefinition);
    
    public ActorPreferences[] getActorPreferences();
    
}
