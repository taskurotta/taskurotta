package ru.taskurotta.client.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 12:11
 */
public class HzTaskSpreaderProvider implements TaskSpreaderProvider {

    private HazelcastInstance hazelcastInstance;

    public HzTaskSpreaderProvider(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public TaskSpreader getTaskSpreader(ActorDefinition actorDefinition) {
        return new HzTaskSpreader(hazelcastInstance, actorDefinition);
    }
}
