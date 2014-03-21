package ru.taskurotta.util;

import ru.taskurotta.core.TaskTarget;

import java.util.ArrayList;
import java.util.List;

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

    public static String getPrefixStripped(String target, String prefix) {
        if (target!=null && target.startsWith(prefix)) {
            return target.substring(prefix.length());
        } else {
            return target;
        }
    }

    public static List<String> getPrefixStripped(List<String> target, String prefix) {
        List<String> result = null;
        if (target!=null && !target.isEmpty()) {
            result = new ArrayList<String>();
            for (String item : target) {
                result.add(getPrefixStripped(item, prefix));
            }
        }
        return result;
    }

//    public static void stripPrefix(Collection<String> target, String prefix) {
//        if (target!=null && !target.isEmpty()) {
//            Iterator<String> iter = target.iterator();
//            while (iter.hasNext()) {
//                String item = iter.next();
//                item = getPrefixStripped(item, prefix);
//            }
//        }
//    }

    public static String toPrefixed(String target, String prefix) {
        if (target != null && prefix!=null && !target.startsWith(prefix)) {
            return prefix + target;
        } else {
            return target;
        }
    }

    public static List<String> toPrefixed(List<String> target, String prefix) {
        List<String> result = null;

        if (target != null && prefix!=null && !target.isEmpty()) {
            result = new ArrayList<String>();
            for (String item: target) {
                result.add(toPrefixed(item, prefix));
            }
        }

        return result;
    }

}
