package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.recovery.IncompleteProcessDao;
import ru.taskurotta.service.recovery.IncompleteProcessesCursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Date: 13.01.14 12:55
 */
public class HzIncompleteProcessDao implements IncompleteProcessDao {

    private static final Logger logger = LoggerFactory.getLogger(HzIncompleteProcessDao.class);

    private IMap<UUID, Process> processIMap;

    private static final String START_TIME_INDEX_NAME = "startTime";
    private static final String STATE_INDEX_NAME = "state";

    public HzIncompleteProcessDao(HazelcastInstance hazelcastInstance, String processesStorageMapName) {
        this.processIMap = hazelcastInstance.getMap(processesStorageMapName);

        processIMap.addIndex(START_TIME_INDEX_NAME, true);
        processIMap.addIndex(STATE_INDEX_NAME, false);
    }

    @Override
    public IncompleteProcessesCursor findProcesses(long timeBefore, int limit) {
        Predicate predicate = new Predicates.AndPredicate(
                new Predicates.BetweenPredicate(START_TIME_INDEX_NAME, 0l, timeBefore),
                new Predicates.EqualPredicate(STATE_INDEX_NAME, Process.START));

        //PagingPredicate should be available in HZ 3.2
        final Collection<UUID> result = new ArrayList<>();
        if (limit > 0) {

            int cnt = 0;
            for (UUID item : processIMap.keySet(predicate)) {
                result.add(item);
                if (++cnt >= limit) {
                    break;
                }
            }
        } else {
            result.addAll(processIMap.keySet(predicate));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Found [{}] incomplete processes for beforeTime[{]]", (result != null ? result.size() : null), timeBefore);
        }

        return new IncompleteProcessesCursor() {
            @Override
            public void close() throws IOException {

            }

            @Override
            public Collection<UUID> getNext() {
                return result;
            }
        };
    }

}
