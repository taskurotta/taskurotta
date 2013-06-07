package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.storage.MemoryTaskDao;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.HazelcastTaskServer;
import ru.taskurotta.server.TaskServer;

/**
 * User: stukushin
 * Date: 06.06.13
 * Time: 16:05
 */
public class HazelcastClientServiceManager implements ClientServiceManager {

    private TaskServer taskServer;
    private BackendBundle backendBundle;

    public HazelcastClientServiceManager() {
        this(Hazelcast.newHazelcastInstance(), 60);
    }

    public HazelcastClientServiceManager(HazelcastInstance hazelcastInstance, int pollDelay) {
        this.backendBundle = new MemoryBackendBundle(pollDelay, new MemoryTaskDao());
        this.taskServer = new HazelcastTaskServer(backendBundle, hazelcastInstance);
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
