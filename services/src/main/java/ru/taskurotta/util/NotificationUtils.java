package ru.taskurotta.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.taskurotta.service.console.model.InterruptedTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created on 09.06.2015.
 */
public class NotificationUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Set<String> asActorIdList(Collection<InterruptedTask> tasks) {
        Set<String> result = null;
        if (tasks!=null && !tasks.isEmpty()) {
            result = new HashSet<>();
            for (InterruptedTask task : tasks) {
                if (task.getActorId()!=null) {
                    result.add(task.getActorId());
                }
                if (task.getStarterId()!=null) {
                    result.add(task.getStarterId());
                }
            }
        }
        return result;
    }

    public static Set<String> getTrackedValues(Collection<String> trackedValues, Collection<String> actualValues) {
        Set<String> result = new HashSet<>();
        if (trackedValues!=null && actualValues!=null) {
            for (String trackedValue : trackedValues) {
                for (String actualValue : actualValues) {
                    if (actualValue.toLowerCase().startsWith(trackedValue.toLowerCase())) {
                        result.add(actualValue);
                    }
                }
            }
        }
        return result.isEmpty()? null : result;
    }

    public static void excludeOldValues(Collection<String> newValues, Collection<String> oldValues) {
        if (newValues!=null && oldValues!=null) {
            Iterator<String> iter = newValues.iterator();
            while (iter.hasNext()) {
                String val = iter.next();
                if (oldValues.contains(val)) {
                    iter.remove();
                }
            }
        }
    }

    public static void excludeOldTasksValues(Collection<InterruptedTask> newValues, Collection<InterruptedTask> oldValues) {
        if (newValues!=null && oldValues!=null) {
            Iterator<InterruptedTask> iter = newValues.iterator();
            while (iter.hasNext()) {
                InterruptedTask val = iter.next();
                if (oldValues.contains(val)) {
                    iter.remove();
                }
            }
        }
    }

    public static String listToJson(List<String> target, String defVal) {
        String result = defVal;
        if (target != null) {
            try {
                result = MAPPER.writeValueAsString(target);
            } catch (Throwable e) {
                throw new RuntimeException("Cannot parse listToJson["+target+"]", e);
            }
        }
        return result;
    }

    public static List<String> jsonToList(String json, List<String> defVal) {
        List<String> result = defVal;
        if (json != null) {
            try {
                result = ((List<String>) MAPPER.readValue(json, new TypeReference<List<String>>(){}));
            } catch (Exception e) {
                throw new RuntimeException("Cannot parse jsonToList ["+json+"]", e);
            }
        }
        return result;
    }

    public static String toCommaDelimited(List<String> target) {
        if (target == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : target) {
            if (sb.length()>0) {
                sb.append(", ");
            }
            sb.append(s);
        }
        return sb.toString();
    }


}
