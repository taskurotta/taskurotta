package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 14:51
 */
public class HzIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(HzIncompleteProcessFinder.class);

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

        long timeBefore = System.currentTimeMillis() - incompleteTimeOutMillis;

        if (logger.isDebugEnabled()) {
            logger.debug("Try to find incomplete processes, started before [{}]", new Date(timeBefore));
        }

        Predicate predicate = new Predicates.AndPredicate(
                new Predicates.BetweenPredicate(startTimeIndexName, 0l, timeBefore),
                new Predicates.EqualPredicate(stateIndexName, Process.START));

        Collection<UUID> processIds = processIMap.localKeySet(predicate);

        if (logger.isInfoEnabled()) {
            logger.info("Found [{}] incomplete processes, started before [{}]", processIds.size(), new Date(timeBefore));
        }

        return processIds;
    }
}
