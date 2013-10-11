package ru.taskurotta.backend.process;

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
    public List<BrokenProcessVO> find(SearchObject searchObject) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
}
