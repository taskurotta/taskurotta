package ru.taskurotta.backend.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.GeneralDependencyBackend;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.hz.checkpoint.CheckpointServiceImpl;
import ru.taskurotta.backend.hz.config.HzConfigBackend;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;
import ru.taskurotta.backend.hz.queue.HzQueueBackend;
import ru.taskurotta.backend.hz.storage.HzProcessBackend;
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


    public HzBackendBundle(int pollDelay, TaskDao taskDao, HazelcastInstance hazelcastInstance) {
        CheckpointServiceImpl checkpointServiceImpl = new CheckpointServiceImpl();
        checkpointServiceImpl.setHzInstance(hazelcastInstance);

        HzProcessBackend hzProcessBackend = new HzProcessBackend(hazelcastInstance);
        hzProcessBackend.setCheckpointService(checkpointServiceImpl);
        this.processBackend = hzProcessBackend;

        this.taskBackend = new GeneralTaskBackend(taskDao, checkpointServiceImpl);

        HzQueueBackend hzQueueBackend = new HzQueueBackend(pollDelay, TimeUnit.SECONDS, hazelcastInstance);
        hzQueueBackend.setCheckpointService(checkpointServiceImpl);
        this.queueBackend = hzQueueBackend;

        this.graphDao = new HzGraphDao(hazelcastInstance);
        this.dependencyBackend = new GeneralDependencyBackend(graphDao, 10);
        this.configBackend = new HzConfigBackend(hazelcastInstance);
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
