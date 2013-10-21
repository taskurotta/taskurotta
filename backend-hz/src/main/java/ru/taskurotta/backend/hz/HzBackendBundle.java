package ru.taskurotta.backend.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.GeneralDependencyBackend;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.hz.config.HzConfigBackend;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;
import ru.taskurotta.backend.hz.queue.HzQueueBackend;
import ru.taskurotta.backend.hz.storage.HzProcessBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.MemoryBrokenProcessBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class HzBackendBundle implements BackendBundle {

    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;
    private GraphDao graphDao;
    private BrokenProcessBackend brokenProcessBackend;

    public HzBackendBundle(int pollDelay, TaskDao taskDao, HazelcastInstance hazelcastInstance) {

        this.processBackend = new HzProcessBackend(hazelcastInstance);

        this.taskBackend = new GeneralTaskBackend(taskDao);

        this.queueBackend = new HzQueueBackend(pollDelay, TimeUnit.SECONDS, hazelcastInstance);

        this.graphDao = new HzGraphDao(hazelcastInstance);
        this.dependencyBackend = new GeneralDependencyBackend(graphDao);
        this.configBackend = new HzConfigBackend(hazelcastInstance, "actorPreferencesMap");
        this.brokenProcessBackend = new MemoryBrokenProcessBackend();
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

    @Override
    public BrokenProcessBackend getBrokenProcessBackend() {
        return brokenProcessBackend;
    }
}
