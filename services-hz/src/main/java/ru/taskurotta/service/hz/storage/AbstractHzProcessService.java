package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ru.taskurotta.service.hz.server.HzTaskServer;
import ru.taskurotta.service.storage.ProcessService;

import java.util.UUID;

/**
 */
public abstract class AbstractHzProcessService implements ProcessService {

    private static final String LOCK_PROCESS_MAP_NAME = HzTaskServer.class.getName() + "#lockProcessMap";

    private IMap<UUID, ?> lockProcessMap;

    public AbstractHzProcessService(HazelcastInstance hzInstance) {
        lockProcessMap = hzInstance.getMap(LOCK_PROCESS_MAP_NAME);
    }

    @Override
    public void lock(UUID processId) {
        lockProcessMap.lock(processId);
    }

    @Override
    public void unlock(UUID processId) {
        lockProcessMap.unlock(processId);
    }
}
