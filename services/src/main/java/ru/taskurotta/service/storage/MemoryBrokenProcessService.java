package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.BrokenProcess;
import ru.taskurotta.service.console.model.SearchCommand;

import java.util.ArrayList;
import java.util.Collection;
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
public class MemoryBrokenProcessService implements BrokenProcessService {

    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> deciderActorIds = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> brokenActorIds = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, CopyOnWriteArraySet<UUID>> times = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> errorMessages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> errorClassNames = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> stackTraces = new ConcurrentHashMap<>();

    private ConcurrentHashMap<UUID, BrokenProcess> brokenProcess = new ConcurrentHashMap<>();

    private static final Lock lock = new ReentrantLock();

    @Override
    public void save(BrokenProcess brokenProcess) {

        UUID processId = brokenProcess.getProcessId();

        addProcessId(deciderActorIds, brokenProcess.getStartActorId(), processId);
        addProcessId(brokenActorIds, brokenProcess.getBrokenActorId(), processId);
        addProcessId(times, brokenProcess.getTime(), processId);
        addProcessId(errorMessages, brokenProcess.getErrorMessage(), processId);
        addProcessId(errorClassNames, brokenProcess.getErrorClassName(), processId);
        addProcessId(stackTraces, brokenProcess.getStackTrace(), processId);

        this.brokenProcess.put(processId, brokenProcess);
    }

    @Override
    public Collection<BrokenProcess> find(SearchCommand searchCommand) {

        if (searchCommand == null) {
            return new ArrayList<>();
        }

        List<UUID> processIds = new ArrayList<>();
        Collection<BrokenProcess> result = new ArrayList<>();

        if (searchCommand.getProcessId() != null) {
            result.add(brokenProcess.get(searchCommand.getProcessId()));
            return result;
        }

        if (searchCommand.getStartActorId() != null) {
            searchByStartString(searchCommand.getStartActorId(), deciderActorIds, processIds);
        }

        if (searchCommand.getBrokenActorId() != null) {
            searchByStartString(searchCommand.getBrokenActorId(), brokenActorIds, processIds);
        }

        if (searchCommand.getStartPeriod() > 0 && searchCommand.getEndPeriod() > 0) {
            Set<Map.Entry<Long, CopyOnWriteArraySet<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, CopyOnWriteArraySet<UUID>> entry: entries) {
                if (entry.getKey() > searchCommand.getStartPeriod() && entry.getKey() < searchCommand.getEndPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        } else if (searchCommand.getStartPeriod() > 0 && searchCommand.getEndPeriod() < 0) {
            Set<Map.Entry<Long, CopyOnWriteArraySet<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, CopyOnWriteArraySet<UUID>> entry: entries) {
                if (entry.getKey() > searchCommand.getStartPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        } else if (searchCommand.getStartPeriod() < 0 && searchCommand.getEndPeriod() > 0) {
            Set<Map.Entry<Long, CopyOnWriteArraySet<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, CopyOnWriteArraySet<UUID>> entry: entries) {
                if (entry.getKey() < searchCommand.getEndPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        }

        if (searchCommand.getErrorMessage() != null) {
            searchByStartString(searchCommand.getErrorMessage(), errorMessages, processIds);
        }

        if (searchCommand.getErrorClassName() != null) {
            searchByStartString(searchCommand.getErrorClassName(), errorClassNames, processIds);
        }

        for (UUID processId : processIds) {
            BrokenProcess brokenProcess = this.brokenProcess.get(processId);
            if (brokenProcess != null) {
                result.add(brokenProcess);
            }
        }

        return result;
    }

    @Override
    public Collection<BrokenProcess> findAll() {
        return brokenProcess.values();
    }

    @Override
    public void delete(UUID processId) {

        if (processId == null) {
            return;
        }

        deleteProcessId(deciderActorIds, processId);
        deleteProcessId(brokenActorIds, processId);
        deleteProcessId(times, processId);
        deleteProcessId(errorMessages, processId);
        deleteProcessId(errorClassNames, processId);
        deleteProcessId(stackTraces, processId);

        brokenProcess.remove(processId);
    }

    @Override
    public void deleteCollection(Collection<UUID> processIds) {
        for (UUID processId : processIds) {
            delete(processId);
        }
    }

    private void addProcessId(ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> map, String key, UUID processId) {

        if (key == null) {
            return;
        }

        CopyOnWriteArraySet<UUID> processIds = map.get(key);

        if (processIds == null) {
            try {
                lock.lock();

                processIds = new CopyOnWriteArraySet<>();
                processIds.add(processId);

                CopyOnWriteArraySet<UUID> previous = map.putIfAbsent(key, processIds);
                if (previous != null) {
                    map.get(key).add(processId);
                }
            } finally {
                lock.unlock();
            }
        } else {
            processIds.add(processId);
        }
    }

    private void addProcessId(ConcurrentHashMap<Long, CopyOnWriteArraySet<UUID>> map, Long key, UUID processId) {

        if (key == null) {
            return;
        }

        CopyOnWriteArraySet<UUID> processIds = map.get(key);

        if (processIds == null) {
            try {
                lock.lock();

                processIds = new CopyOnWriteArraySet<>();
                processIds.add(processId);

                CopyOnWriteArraySet<UUID> previous = map.putIfAbsent(key, processIds);
                if (previous != null) {
                    map.get(key).add(processId);
                }
            } finally {
                lock.unlock();
            }
        } else {
            processIds.add(processId);
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

    private void deleteProcessId(ConcurrentHashMap<?, CopyOnWriteArraySet<UUID>> map, UUID processId) {

        if (processId == null) {
            return;
        }

        Collection<CopyOnWriteArraySet<UUID>> values = map.values();
        for (CopyOnWriteArraySet<UUID> processIds : values) {
            processIds.remove(processId);
        }
    }
}
