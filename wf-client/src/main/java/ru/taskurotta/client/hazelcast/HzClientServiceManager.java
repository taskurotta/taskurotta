package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.hz.HzBackendBundle;
import ru.taskurotta.backend.hz.server.HzTaskServerV2;
import ru.taskurotta.backend.hz.storage.HzTaskDao;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.TaskServer;

/**
 * User: stukushin
 * Date: 06.06.13
 * Time: 16:05
 */
public class HzClientServiceManager implements ClientServiceManager {

    private TaskServer taskServer;
    private BackendBundle backendBundle;

    public HzClientServiceManager() {
        this(Hazelcast.newHazelcastInstance(), 60);
    }

    public HzClientServiceManager(HazelcastInstance hazelcastInstance, int pollDelay) {
        this.backendBundle = new HzBackendBundle(pollDelay, new HzTaskDao(hazelcastInstance), hazelcastInstance);
        this.taskServer = new HzTaskServerV2(backendBundle, hazelcastInstance);
    }

    @Override
    public DeciderClientProvider getDeciderClientProvider() {
        return new DeciderClientProviderCommon(taskServer);
    }

    @Override
    public TaskSpreaderProvider getTaskSpreaderProvider() {
        return new TaskSpreaderProviderCommon(taskServer);
    }
}
