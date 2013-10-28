package ru.taskurotta.backend.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:28
 */
public class MemoryBrokenProcessBackend implements BrokenProcessBackend {

    private Map<String, Set<UUID>> deciderActorIds = new HashMap<>();
    private Map<String, Set<UUID>> brokenActorIds = new HashMap<>();
    private Map<Long, Set<UUID>> times = new HashMap<>();
    private Map<String, Set<UUID>> errorMessages = new HashMap<>();
    private Map<String, Set<UUID>> errorClassNames = new HashMap<>();
    private Map<String, Set<UUID>> stackTraces = new HashMap<>();

    private Map<UUID, BrokenProcessVO> brokenProcess = new HashMap<>();

    @Override
    public void save(BrokenProcessVO brokenProcessVO) {

        UUID processId = brokenProcessVO.getProcessId();

        addProcessId(deciderActorIds, brokenProcessVO.getStartActorId(), processId);
        addProcessId(brokenActorIds, brokenProcessVO.getBrokenActorId(), processId);
        addProcessId(times, brokenProcessVO.getTime(), processId);
        addProcessId(errorMessages, brokenProcessVO.getErrorMessage(), processId);
        addProcessId(errorClassNames, brokenProcessVO.getErrorClassName(), processId);
        addProcessId(stackTraces, brokenProcessVO.getStackTrace(), processId);

        brokenProcess.put(processId, brokenProcessVO);
    }

    @Override
    public Collection<BrokenProcessVO> find(SearchCommand searchCommand) {

        if (searchCommand == null) {
            return new ArrayList<>();
        }

        List<UUID> processIds = new ArrayList<>();
        Collection<BrokenProcessVO> result = new ArrayList<>();

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
            Set<Map.Entry<Long, Set<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, Set<UUID>> entry: entries) {
                if (entry.getKey() > searchCommand.getStartPeriod() && entry.getKey() < searchCommand.getEndPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        } else if (searchCommand.getStartPeriod() > 0 && searchCommand.getEndPeriod() < 0) {
            Set<Map.Entry<Long, Set<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, Set<UUID>> entry: entries) {
                if (entry.getKey() > searchCommand.getStartPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        } else if (searchCommand.getStartPeriod() < 0 && searchCommand.getEndPeriod() > 0) {
            Set<Map.Entry<Long, Set<UUID>>> entries = times.entrySet();
            for (Map.Entry<Long, Set<UUID>> entry: entries) {
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
            BrokenProcessVO brokenProcessVO = brokenProcess.get(processId);
            if (brokenProcessVO != null) {
                result.add(brokenProcessVO);
            }
        }

        return result;
    }

    @Override
    public Collection<BrokenProcessVO> findAll() {
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

    private void addProcessId(Map<String, Set<UUID>> map, String key, UUID processId) {
        Set<UUID> processIds = map.get(key);

        if (processIds == null) {
            processIds = new HashSet<>();
            processIds.add(processId);
            map.put(key, processIds);
        } else {
            processIds.add(processId);
        }
    }

    private void addProcessId(Map<Long, Set<UUID>> map, Long key, UUID processId) {
        Set<UUID> processIds = map.get(key);

        if (processIds == null) {
            processIds = new HashSet<>();
            processIds.add(processId);
            map.put(key, processIds);
        } else {
            processIds.add(processId);
        }
    }

    private void searchByStartString(String prefix, Map<String, Set<UUID>> map, Collection<UUID> result) {
        Set<Map.Entry<String, Set<UUID>>> entries = map.entrySet();

        for (Map.Entry<String, Set<UUID>> entry : entries) {
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

    private void deleteProcessId(Map<?, Set<UUID>> map, UUID processId) {

        if (processId == null) {
            return;
        }

        Collection<Set<UUID>> values = map.values();
        for (Set<UUID> processIds : values) {
            processIds.remove(processId);
        }
    }
}
