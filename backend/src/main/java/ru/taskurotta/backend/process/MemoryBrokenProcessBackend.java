package ru.taskurotta.backend.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:28
 */
public class MemoryBrokenProcessBackend implements BrokenProcessBackend {

    private Map<String, Set<String>> deciderActorIds = new HashMap<>();
    private Map<String, Set<String>> brokenActorIds = new HashMap<>();
    private Map<Long, Set<String>> times = new HashMap<>();
    private Map<String, Set<String>> errorMessages = new HashMap<>();
    private Map<String, Set<String>> errorClassNames = new HashMap<>();
    private Map<String, Set<String>> stackTraces = new HashMap<>();

    private Map<String, BrokenProcessVO> brokenProcess = new HashMap<>();

    @Override
    public void save(BrokenProcessVO brokenProcessVO) {

        String processId = brokenProcessVO.getProcessId();

        addProcessId(deciderActorIds, brokenProcessVO.getDeciderActorId(), processId);
        addProcessId(brokenActorIds, brokenProcessVO.getBrokenActorId(), processId);
        addProcessId(times, brokenProcessVO.getTime(), processId);
        addProcessId(errorMessages, brokenProcessVO.getErrorMessage(), processId);
        addProcessId(errorClassNames, brokenProcessVO.getErrorClassName(), processId);
        addProcessId(stackTraces, brokenProcessVO.getStackTrace(), processId);

        brokenProcess.put(processId, brokenProcessVO);
    }

    @Override
    public Collection<BrokenProcessVO> find(SearchObject searchObject) {

        if (searchObject == null) {
            return new ArrayList<>();
        }

        List<String> processIds = new ArrayList<>();
        Collection<BrokenProcessVO> result = new ArrayList<>();

        if (searchObject.getProcessId() != null) {
            result.add(brokenProcess.get(searchObject.getProcessId()));
            return result;
        }

        if (searchObject.getDeciderActorId() != null) {
            searchByStartString(searchObject.getDeciderActorId(), deciderActorIds, processIds);
        }

        if (searchObject.getBrokenActorId() != null) {
            searchByStartString(searchObject.getBrokenActorId(), brokenActorIds, processIds);
        }

        if (searchObject.getStartPeriod() > 0 && searchObject.getEndPeriod() > 0) {
            Set<Map.Entry<Long, Set<String>>> entries = times.entrySet();
            for (Map.Entry<Long, Set<String>> entry: entries) {
                if (entry.getKey() > searchObject.getStartPeriod() && entry.getKey() < searchObject.getEndPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        } else if (searchObject.getStartPeriod() > 0 && searchObject.getEndPeriod() < 0) {
            Set<Map.Entry<Long, Set<String>>> entries = times.entrySet();
            for (Map.Entry<Long, Set<String>> entry: entries) {
                if (entry.getKey() > searchObject.getStartPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        } else if (searchObject.getStartPeriod() < 0 && searchObject.getEndPeriod() > 0) {
            Set<Map.Entry<Long, Set<String>>> entries = times.entrySet();
            for (Map.Entry<Long, Set<String>> entry: entries) {
                if (entry.getKey() < searchObject.getEndPeriod()) {
                    merge(entry.getValue(), processIds);
                }
            }
        }

        if (searchObject.getErrorMessage() != null) {
            searchByStartString(searchObject.getErrorMessage(), errorMessages, processIds);
        }

        if (searchObject.getErrorClassName() != null) {
            searchByStartString(searchObject.getErrorClassName(), errorClassNames, processIds);
        }

        for (String processId : processIds) {
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

    private void addProcessId(Map<String, Set<String>> map, String key, String processId) {
        Set<String> processIds = map.get(key);

        if (processIds == null) {
            processIds = new HashSet<>();
            processIds.add(processId);
            map.put(key, processIds);
        } else {
            processIds.add(processId);
        }
    }

    private void addProcessId(Map<Long, Set<String>> map, Long key, String processId) {
        Set<String> processIds = map.get(key);

        if (processIds == null) {
            processIds = new HashSet<>();
            processIds.add(processId);
            map.put(key, processIds);
        } else {
            processIds.add(processId);
        }
    }

    private void searchByStartString(String prefix, Map<String, Set<String>> map, Collection<String> result) {
        Set<Map.Entry<String, Set<String>>> entries = map.entrySet();

        for (Map.Entry<String, Set<String>> entry : entries) {
            if (entry.getKey().startsWith(prefix)) {
                merge(entry.getValue(), result);
            }
        }
    }

    private Collection<String> merge(Collection<String> from, Collection<String> to) {

        for (String strFrom : from) {
            if (to.contains(strFrom)) {
                continue;
            }

            to.add(strFrom);
        }

        return to;
    }
}
