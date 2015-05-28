package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.InterruptedTaskExt;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;
import ru.taskurotta.util.InterruptedTaskSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created: 11.10.13 18:28
 */
@Deprecated
public class MemoryInterruptedTasksService implements InterruptedTasksService {

    private ConcurrentHashMap<UUID, InterruptedTaskExt> brokenTasks = new ConcurrentHashMap<>();

    @Override
    public void save(InterruptedTask itdTask, String message, String stackTrace) {
        UUID taskId = itdTask.getTaskId();
        this.brokenTasks.put(taskId, new InterruptedTaskExt(itdTask, message, stackTrace));
    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand com) {
        Collection<InterruptedTask> result = new ArrayList<>();
        if (com != null) {
            for (InterruptedTaskExt task : brokenTasks.values()) {
                if (task == null) {
                    continue;
                }
                if ( (com.getProcessId()==null || com.getProcessId().equals(task.getProcessId()))
                        && (com.getActorId()==null || startsWith(task.getActorId(), com.getActorId()))
                        && (com.getStarterId()==null || startsWith(task.getStarterId(), com.getStarterId()))
                        && (com.getErrorClassName()==null || startsWith(task.getErrorClassName(), com.getErrorClassName()))
                        && (com.getErrorMessage() == null || startsWith(task.getErrorMessage(), com.getErrorMessage()))
                        && ( (com.getStartPeriod()<=0&&com.getEndPeriod()<=0) || isInsideTargetPeriod(com.getStartPeriod(), com.getEndPeriod(), task.getTime()))) {
                    result.add(task);
                }
            }
        }
        return result;
    }

    boolean isInsideTargetPeriod(long start, long end, long target) {
        boolean result = false;
        if (start>0&&end>0) {
            result = target>=start && target<=end;
        } else if (start>0 && end<0) {
            result = target>=start;
        } else if (start<0 && end>0) {
            result = target<=end;
        }
        return result;
    }

    boolean startsWith(String target, String prefix) {
        if (target!=null && prefix!=null && target.trim().toUpperCase().startsWith(prefix.trim().toUpperCase())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        Collection<InterruptedTask> result = null;

        Collection<InterruptedTaskExt> tasks = brokenTasks.values();
        if (tasks!=null && !tasks.isEmpty()) {
            result = new ArrayList<>();
            for (InterruptedTaskExt itdTask : tasks) {
                result.add(itdTask);
            }
        }
        return result;
    }

    @Override
    public void delete(UUID processId, UUID taskId) {

        if (taskId == null) {
            return;
        }
        brokenTasks.remove(taskId);
    }

    @Override
    public String getFullMessage(UUID processId, UUID taskId) {
        Iterator<InterruptedTaskExt> iter = brokenTasks.values().iterator();
        while (iter.hasNext()) {
            InterruptedTaskExt task = iter.next();
            if (task!=null && task.getProcessId()!=null
                    && task.getProcessId().equals(processId)
                    && task.getTaskId()!=null
                    && task.getTaskId().equals(taskId)) {
                return task.getFullMessage();
            }
        }
        return null;
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        Iterator<InterruptedTaskExt> iter = brokenTasks.values().iterator();
        while (iter.hasNext()) {
            InterruptedTaskExt task = iter.next();
            if (task!=null && task.getProcessId()!=null
                    && task.getProcessId().equals(processId)
                    && task.getTaskId()!=null
                    && task.getTaskId().equals(taskId)) {
                return task.getStackTrace();
            }
        }
        return null;
    }

    @Override
    public List<TasksGroupVO> getGroupList(GroupCommand command) {
        List<TasksGroupVO> result = null;
        Collection<InterruptedTask> tasks = find(command);

        if (tasks != null && !tasks.isEmpty()) {
            Map<String, Collection<InterruptedTask>> groupedTasks = InterruptedTaskSupport.groupProcessList(tasks, command.getGroup());
            result = InterruptedTaskSupport.convertToGroupsList(groupedTasks, command);
        }

        return result;
    }

    @Override
    public Collection<TaskIdentifier> getTaskIdentifiers(GroupCommand command) {
        return InterruptedTaskSupport.asTaskIdentifiers(find(command), command);
    }

    @Override
    public Set<UUID> getProcessIds(GroupCommand command) {
        return InterruptedTaskSupport.asProcessIdentifiers(find(command), command);
    }

    @Override
    public long deleteTasksForProcess(UUID processId) {
        long result = 0l;
        if (processId != null) {
            synchronized (brokenTasks) {
                Iterator<Map.Entry<UUID, InterruptedTaskExt>> iter = brokenTasks.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, InterruptedTaskExt> entry = iter.next();
                    if (entry!=null && entry.getValue()!=null && processId.equals(entry.getValue().getProcessId())) {
                        iter.remove();
                        result++;
                    }
                }
            }
        }
        return result;
    }

}
