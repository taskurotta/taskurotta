package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;

import java.util.Collection;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 14:51
 */
public class HzIncompleteProcessFinder implements IncompleteProcessFinder {

    private IMap<UUID, Process> processIMap;

    private static final String startTimeIndexName = "startTime";
    private static final String stateIndexName = "state";

    public HzIncompleteProcessFinder(HazelcastInstance hazelcastInstance, String processesStorageMapName) {
        this.processIMap = hazelcastInstance.getMap(processesStorageMapName);

        processIMap.addIndex(startTimeIndexName, true);
        processIMap.addIndex(stateIndexName, false);
    }

    @Override
    public Collection<UUID> find(long incompleteTimeOutMillis) {

        Predicate predicate = new Predicates.AndPredicate(
                new Predicates.BetweenPredicate(startTimeIndexName, 0l, System.currentTimeMillis() - incompleteTimeOutMillis),
                new Predicates.EqualPredicate(stateIndexName, Process.START));

        Collection<UUID> processIds = processIMap.localKeySet(predicate);

        if (processIds == null || processIds.isEmpty()) {
            return null;
        }

        return processIds;
    }
}
