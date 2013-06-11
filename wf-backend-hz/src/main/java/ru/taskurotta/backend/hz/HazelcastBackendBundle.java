package ru.taskurotta.backend.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.MemoryGraphDao;
import ru.taskurotta.backend.hz.dependency.HazelcastDependencyBackend;
import ru.taskurotta.backend.queue.MemoryQueueBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.MemoryProcessBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;

/**
 * User: stukushin
 * Date: 10.06.13
 * Time: 16:13
 */
public class HazelcastBackendBundle implements BackendBundle {

    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;
    private MemoryGraphDao memoryGraphDao;

    public HazelcastBackendBundle(HazelcastInstance hazelcastInstance, TaskDao taskDao, int pollDelay) {
        this.processBackend = new MemoryProcessBackend();
        this.taskBackend = new GeneralTaskBackend(taskDao, new MemoryCheckpointService());
        this.queueBackend = new MemoryQueueBackend(pollDelay);
        this.memoryGraphDao = new MemoryGraphDao();
        this.dependencyBackend = new HazelcastDependencyBackend(hazelcastInstance, memoryGraphDao);
        this.configBackend = new MemoryConfigBackend();
    }

    @Override
    public ProcessBackend getProcessBackend() {
        return processBackend;
    }

    @Override
    public TaskBackend getTaskBackend() {
        return taskBackend;
    }

    @Override
    public QueueBackend getQueueBackend() {
        return queueBackend;
    }

    @Override
    public DependencyBackend getDependencyBackend() {
        return dependencyBackend;
    }

    @Override
    public ConfigBackend getConfigBackend() {
        return configBackend;
    }
}
