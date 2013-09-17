package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 12:12
 */
public class HzClientServiceManagerV2 implements ClientServiceManager {

    private HazelcastInstance hazelcastInstance;

    public HzClientServiceManagerV2(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public DeciderClientProvider getDeciderClientProvider() {
        return new HzDeciderClientProvider(hazelcastInstance);
    }

    @Override
    public TaskSpreaderProvider getTaskSpreaderProvider() {
        return new HzTaskSpreaderProvider(hazelcastInstance);
    }
}
