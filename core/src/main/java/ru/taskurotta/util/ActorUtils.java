package ru.taskurotta.util;

import ru.taskurotta.core.TaskTarget;

public class ActorUtils {

    public static final String SEPARATOR = "#";

    public static String getActorId(ActorDefinition actorDefinition) {
        return actorDefinition.getName() + SEPARATOR + actorDefinition.getVersion();
    }

    public static String getFullActorName(ActorDefinition actorDefinition) {
        String taskList = actorDefinition.getTaskList();

        if (taskList != null) {
            return actorDefinition.getName() + SEPARATOR +
                    actorDefinition.getVersion() + SEPARATOR +
                    actorDefinition.getTaskList();
        }

        return actorDefinition.getName() + SEPARATOR +
                actorDefinition.getVersion();
    }

    public static ActorDefinition getActorDefinition(String actorId) {
        int firstSeparatorIndex = actorId.indexOf(SEPARATOR);
        return ActorDefinition.valueOf(actorId.substring(0, firstSeparatorIndex),
                actorId.substring(firstSeparatorIndex + 1));
    }

    public static String getActorId(TaskTarget taskTarget) {
        return taskTarget.getName() + SEPARATOR + taskTarget.getVersion();
    }

    public static String getPrefixStripped(String target, String prefix) {
        if (target != null && target.startsWith(prefix)) {
            return target.substring(prefix.length());
        } else {
            return target;
        }
    }

    public static String toPrefixed(String target, String prefix) {
        if (target != null && prefix != null && !target.startsWith(prefix)) {
            return prefix + target;
        } else {
            return target;
        }
    }

}
