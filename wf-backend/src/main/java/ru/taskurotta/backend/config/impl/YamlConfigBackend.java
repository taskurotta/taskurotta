package ru.taskurotta.backend.config.impl;

import java.util.Arrays;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.util.ActorDefinition;

/**
 * Config implementation provided by DropWizard configuration file  
 */
public class YamlConfigBackend implements ConfigBackend {

	private ActorPreferences[] actorPreferences;
	
	@Override
	public boolean isActorBlocked(ActorDefinition actorDefinition) {
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
	public String toString() {
		return "DwYamlConfigBackendImpl [actorPreferences="
				+ Arrays.toString(actorPreferences) + "]";
	}
	
}
