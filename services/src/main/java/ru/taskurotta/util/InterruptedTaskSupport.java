package ru.taskurotta.util;

import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created on 26.05.2015.
 */
public class InterruptedTaskSupport {

    public static Map<String, Collection<InterruptedTask>> groupProcessList(Collection<InterruptedTask> tasks, String groupType) {
        Map<String, Collection<InterruptedTask>> result = new HashMap<>();

        if (tasks != null && !tasks.isEmpty()) {

            if (GroupCommand.GROUP_ACTOR.equals(groupType)) {
                for (InterruptedTask it : tasks) {
                    Collection<InterruptedTask> coll = result.get(it.getActorId());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(it);
                    result.put(it.getActorId(), coll);
                }
            } else if (GroupCommand.GROUP_EXCEPTION.equals(groupType)) {
                for (InterruptedTask it : tasks) {
                    Collection<InterruptedTask> coll = result.get(it.getErrorClassName());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(it);
                    result.put(it.getErrorClassName(), coll);
                }
            } else {
                for (InterruptedTask it : tasks) {
                    Collection<InterruptedTask> coll = result.get(it.getStarterId());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(it);
                    result.put(it.getStarterId(), coll);
                }
            }

        }

        return result;
    }

    public static List<TasksGroupVO> convertToGroupsList(Map<String, Collection<InterruptedTask>> groupedProcesses, GroupCommand command) {
        List<TasksGroupVO> result = null;
        if (groupedProcesses != null && !groupedProcesses.isEmpty()) {
            result = new ArrayList<>();
            for (Map.Entry<String, Collection<InterruptedTask>> entry : groupedProcesses.entrySet()) {
                Collection<InterruptedTask> groupItems = entry.getValue();
                TasksGroupVO group = convertToGroup(groupItems, entry.getKey());
                result.add(group);
            }
        }
        return result;
    }

    public static TasksGroupVO convertToGroup(Collection<InterruptedTask> members, String name) {
        TasksGroupVO group = new TasksGroupVO();
        Set<String> actorsDiffs = new HashSet<>();
        Set<String> startersDiffs = new HashSet<>();
        Set<String> exceptionsDiffs = new HashSet<>();
        Set<TaskIdentifier> tasks = new HashSet<>();
        if (members != null && !members.isEmpty()) {
            for (InterruptedTask it : members) {
                actorsDiffs.add(it.getActorId());
                startersDiffs.add(it.getStarterId());
                exceptionsDiffs.add(it.getErrorClassName());
                tasks.add(new TaskIdentifier(it.getTaskId(), it.getProcessId()));
            }
        }
        group.setName(name);
        group.setStartersCount(startersDiffs.size());
        group.setActorsCount(actorsDiffs.size());
        group.setExceptionsCount(exceptionsDiffs.size());
        group.setTotal(tasks.size());

        return group;
    }

    public static Collection<TaskIdentifier> asTaskIdentifiers(Collection<InterruptedTask> tasks, GroupCommand command) {
        Collection<TaskIdentifier> result = new ArrayList<>();
        if (command.getGroup() != null) {
            if (tasks!=null && !tasks.isEmpty()) {
                for (InterruptedTask task : tasks) {
                    if ( (GroupCommand.GROUP_ACTOR.equalsIgnoreCase(command.getGroup()) && command.getActorId().equals(task.getActorId()))
                            || (GroupCommand.GROUP_EXCEPTION.equalsIgnoreCase(command.getGroup()) && command.getErrorClassName().equals(task.getErrorClassName()))
                            || (GroupCommand.GROUP_STARTER.equalsIgnoreCase(command.getGroup()) && command.getStarterId().equals(task.getStarterId())) ) {
                        result.add(new TaskIdentifier(task.getTaskId(), task.getProcessId()));
                    }
                }
            }
        }
        return result.isEmpty()? null : result;
    }

    public static Set<UUID> asProcessIdentifiers(Collection<InterruptedTask> tasks, GroupCommand command) {
        Set<UUID> result = new HashSet<>();
        if (command.getGroup() != null) {
            if (tasks!=null && !tasks.isEmpty()) {
                for (InterruptedTask task : tasks) {
                    if ( (GroupCommand.GROUP_ACTOR.equalsIgnoreCase(command.getGroup()) && command.getActorId().equals(task.getActorId()))
                            || (GroupCommand.GROUP_EXCEPTION.equalsIgnoreCase(command.getGroup()) && command.getErrorClassName().equals(task.getErrorClassName()))
                            || (GroupCommand.GROUP_STARTER.equalsIgnoreCase(command.getGroup()) && command.getStarterId().equals(task.getStarterId())) ) {
                        result.add(task.getProcessId());
                    }
                }
            }
        }
        return result.isEmpty()? null : result;
    }

}
