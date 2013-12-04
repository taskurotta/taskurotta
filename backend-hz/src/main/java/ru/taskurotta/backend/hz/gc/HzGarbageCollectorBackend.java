package ru.taskurotta.backend.hz.gc;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.gc.GarbageCollectorBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.UUID;

public class HzGarbageCollectorBackend implements GarbageCollectorBackend {

    private static final Logger logger = LoggerFactory.getLogger(HzGarbageCollectorBackend.class);

    private ConfigBackend configBackend;
    private ProcessBackend processBackend;
    private GraphDao graphDao;
    private TaskDao taskDao;

    private HazelcastInstance hazelcastInstance;
    private String garbageCollectorQueueName = "garbageCollectorQueue";

    private IQueue<UUID> garbageCollectorQueue;

    public HzGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, HazelcastInstance hazelcastInstance) {
        this(configBackend, processBackend, graphDao, taskDao, hazelcastInstance, "garbageCollectorQueue", 1);
    }

    public HzGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, HazelcastInstance hazelcastInstance, String garbageCollectorQueueName) {
        this(configBackend, processBackend, graphDao, taskDao, hazelcastInstance, garbageCollectorQueueName, 1);
    }

    public HzGarbageCollectorBackend(ConfigBackend configBackend, ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao, HazelcastInstance hazelcastInstance, String garbageCollectorQueueName, int poolSize) {
        this.configBackend = configBackend;
        this.processBackend = processBackend;
        this.graphDao = graphDao;
        this.taskDao = taskDao;
        this.hazelcastInstance = hazelcastInstance;
        this.garbageCollectorQueueName = garbageCollectorQueueName;

        this.garbageCollectorQueue = hazelcastInstance.getQueue(garbageCollectorQueueName);
    }

    @Override
    public void delete(UUID processId, String actorId) {
        ActorPreferences actorPreferences = configBackend.getActorPreferences(actorId);

        long keepTime = 0;
        if (actorPreferences != null) {
            keepTime = actorPreferences.getKeepTime();
        }

        garbageCollectorQueue.add(processId);
    }
}
