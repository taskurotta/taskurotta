package ru.taskurotta.util;

import ru.taskurotta.core.TaskTarget;

public class ActorUtils {
	
	public static final String SEPARATOR = "#";
	
	public static String getActorId(ActorDefinition actorDefinition) {
		return actorDefinition.getName() + SEPARATOR + actorDefinition.getVersion();
	}
	
	public static ActorDefinition getActorDefinition(String actorId) {
		int firstSeparatorIndex = actorId.indexOf(SEPARATOR);
		return ActorDefinition.valueOf(actorId.substring(0, firstSeparatorIndex), actorId.substring(firstSeparatorIndex+1));
	}
	
	public static String getActorId(TaskTarget taskTarget) {
		return taskTarget.getName() + SEPARATOR + taskTarget.getVersion();
	}
	
	
}
