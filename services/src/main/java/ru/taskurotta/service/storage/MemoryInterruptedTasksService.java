package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.InterruptedTaskExt;
import ru.taskurotta.service.console.model.SearchCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:28
 */
public class MemoryInterruptedTasksService implements InterruptedTasksService {

    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> actorIds = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> starterIds = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> processIds = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, CopyOnWriteArraySet<UUID>> times = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> errorMessages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> errorClassNames = new ConcurrentHashMap<>();

    private ConcurrentHashMap<UUID, InterruptedTaskExt> brokenTasks = new ConcurrentHashMap<>();

    private static final Lock lock = new ReentrantLock();

    @Override
    public void save(InterruptedTask itdTask, String message, String stackTrace) {

        UUID taskId = itdTask.getTaskId();

        String processId = itdTask.getProcessId()!=null? itdTask.getProcessId().toString() : null;

        addTaskId(actorIds, itdTask.getActorId(), taskId);
        addTaskId(starterIds, itdTask.getStarterId(), taskId);
        addTaskId(processIds, processId, taskId);
        addTaskId(times, itdTask.getTime(), taskId);
        addTaskId(errorMessages, itdTask.getErrorMessage(), taskId);
        addTaskId(errorClassNames, itdTask.getErrorClassName(), taskId);

        this.brokenTasks.put(taskId, new InterruptedTaskExt(itdTask, message, stackTrace));
    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand searchCommand) {

        if (searchCommand == null) {
            return new ArrayList<>();
        }

        List<UUID> taskIds = new ArrayList<>();
        Collection<InterruptedTask> result = new ArrayList<>();

        if (searchCommand.getProcessId() != null) {
            result.add(brokenTasks.get(searchCommand.getProcessId()));
            return result;
        }

        if (searchCommand.getActorId() != null) {
            searchByStartString(searchCommand.getActorId(), actorIds, taskIds);
        }

        if (searchCommand.getStarterId() != null) {
            searchByStartString(searchCommand.getStarterId(), starterIds, taskIds);
        }

        if (searchCommand.getProcessId() != null) {
            searchByStartString(searchCommand.getProcessId().toString(), this.processIds, taskIds);
        }

        if (searchCommand.getStartPeriod() > 0 && searchCommand.getEndPeriod() > 0) {
            Set<Map.Entry<Long, CopyOnWriteArraySet<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, CopyOnWriteArraySet<UUID>> entry: entries) {
                if (entry.getKey() > searchCommand.getStartPeriod() && entry.getKey() < searchCommand.getEndPeriod()) {
                    merge(entry.getValue(), taskIds);
                }
            }
        } else if (searchCommand.getStartPeriod() > 0 && searchCommand.getEndPeriod() < 0) {
            Set<Map.Entry<Long, CopyOnWriteArraySet<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, CopyOnWriteArraySet<UUID>> entry: entries) {
                if (entry.getKey() > searchCommand.getStartPeriod()) {
                    merge(entry.getValue(), taskIds);
                }
            }
        } else if (searchCommand.getStartPeriod() < 0 && searchCommand.getEndPeriod() > 0) {
            Set<Map.Entry<Long, CopyOnWriteArraySet<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, CopyOnWriteArraySet<UUID>> entry: entries) {
                if (entry.getKey() < searchCommand.getEndPeriod()) {
                    merge(entry.getValue(), taskIds);
                }
            }
        }

        if (searchCommand.getErrorMessage() != null) {
            searchByStartString(searchCommand.getErrorMessage(), errorMessages, taskIds);
        }

        if (searchCommand.getErrorClassName() != null) {
            searchByStartString(searchCommand.getErrorClassName(), errorClassNames, taskIds);
        }

        for (UUID taskId : taskIds) {
            InterruptedTask itdTask = this.brokenTasks.get(taskId);
            if (itdTask != null) {
                result.add(itdTask);
            }
        }

        return result;
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

        deleteTaskId(actorIds, taskId);
        deleteTaskId(starterIds, taskId);
        deleteTaskId(processIds, taskId);
        deleteTaskId(times, taskId);
        deleteTaskId(errorMessages, taskId);
        deleteTaskId(errorClassNames, taskId);

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

    private void addTaskId(ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> map, String key, UUID taskId) {

        if (key == null) {
            return;
        }

        CopyOnWriteArraySet<UUID> taskIds = map.get(key);

        if (taskIds == null) {
            try {
                lock.lock();

                taskIds = new CopyOnWriteArraySet<>();
                taskIds.add(taskId);

                CopyOnWriteArraySet<UUID> previous = map.putIfAbsent(key, taskIds);
                if (previous != null) {
                    map.get(key).add(taskId);
                }
            } finally {
                lock.unlock();
            }
        } else {
            taskIds.add(taskId);
        }
    }

    private void addTaskId(ConcurrentHashMap<Long, CopyOnWriteArraySet<UUID>> map, Long key, UUID taskId) {

        if (key == null) {
            return;
        }

        CopyOnWriteArraySet<UUID> taskIds = map.get(key);

        if (taskIds == null) {
            try {
                lock.lock();

                taskIds = new CopyOnWriteArraySet<>();
                taskIds.add(taskId);

                CopyOnWriteArraySet<UUID> previous = map.putIfAbsent(key, taskIds);
                if (previous != null) {
                    map.get(key).add(taskId);
                }
            } finally {
                lock.unlock();
            }
        } else {
            taskIds.add(taskId);
        }
    }

    private void searchByStartString(String prefix, ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> map, Collection<UUID> result) {
        Set<Map.Entry<String, CopyOnWriteArraySet<UUID>>> entries = map.entrySet();

        for (Map.Entry<String, CopyOnWriteArraySet<UUID>> entry : entries) {
            if (entry.getKey().startsWith(prefix)) {
                merge(entry.getValue(), result);
            }
        }
    }

    private Collection<UUID> merge(Collection<UUID> from, Collection<UUID> to) {

        for (UUID uuid : from) {
            if (to.contains(uuid)) {
                continue;
            }

            to.add(uuid);
        }

        return to;
    }

    private void deleteTaskId(ConcurrentHashMap<?, CopyOnWriteArraySet<UUID>> map, UUID taskId) {

        if (taskId == null) {
            return;
        }

        Collection<CopyOnWriteArraySet<UUID>> values = map.values();
        for (CopyOnWriteArraySet<UUID> processIds : values) {
            processIds.remove(taskId);
        }
    }
}
