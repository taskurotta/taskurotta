package ru.taskurotta.util;

import ru.taskurotta.service.console.model.InterruptedTask;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created on 09.06.2015.
 */
public class NotificationUtils {

    public static boolean containsValueOfInterest(Collection<String> target, Collection<String> valuesOfInterest) {
        boolean result = false;
        if (target!=null && valuesOfInterest!=null) {
            for (String item : target) {
                for(String valueOfInterest : valuesOfInterest) {
                    if (valueOfInterest.toLowerCase().startsWith(item.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return result;
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
