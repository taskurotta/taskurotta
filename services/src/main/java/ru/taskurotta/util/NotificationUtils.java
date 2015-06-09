package ru.taskurotta.util;

import ru.taskurotta.service.console.model.InterruptedTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created on 09.06.2015.
 */
public class NotificationUtils {

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

    public static Set<String> getTrackedValues(Collection<String> target, Collection<String> valuesOfInterest) {
        Set<String> result = new HashSet<>();
        if (target!=null && valuesOfInterest!=null) {
            for (String item : target) {
                for (String valueOfInterest : valuesOfInterest) {
                    if (valueOfInterest.toLowerCase().startsWith(item.toLowerCase())) {
                        result.add(valueOfInterest);
                    }
                }
            }
        }
        return result.isEmpty()? null : result;
    }

    public static Collection<String> getFilteredQueueValues(Collection<String> target, Collection<String> stored) {
        Collection<String> result = target;
        if (target!=null && stored!=null) {
            result = new ArrayList<>();
            for (String val : target) {
                if (!stored.contains(val)) {
                    result.add(val);
                }
            }
        }
        return result;
    }

    public static Collection<InterruptedTask> getFilteredTaskValues(Collection<InterruptedTask> target, Collection<InterruptedTask> stored) {
        Collection<InterruptedTask> result = target;
        if (target!=null && stored != null) {
            result = new ArrayList<InterruptedTask>();
            for (InterruptedTask task : target) {
                if (!stored.contains(task)) {
                    result.add(task);
                }
            }
        }
        return result;
    }



}
