package ru.taskurotta.backend.config.impl;

import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.util.ActorDefinition;

public class MemoryConfigBackend implements ConfigBackend {

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
	
}
