package ru.taskurotta.service.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.hazelcast.queue.delay.DefaultQueueFactory;
import ru.taskurotta.hazelcast.queue.delay.DefaultStorageFactory;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;
import ru.taskurotta.hazelcast.queue.delay.StorageFactory;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.GeneralDependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.hz.config.HzConfigService;
import ru.taskurotta.service.hz.dependency.HzGraphDao;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.hz.storage.HzInterruptedTasksService;
import ru.taskurotta.service.hz.storage.HzProcessService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.*;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class HzServiceBundle implements ServiceBundle {

    private ProcessService processService;
    private TaskService taskService;
    private QueueService queueService;
    private DependencyService dependencyService;
    private ConfigService configService;
    private GraphDao graphDao;
    private InterruptedTasksService interruptedTasksService;
    private GarbageCollectorService garbageCollectorService;

    public HzServiceBundle(int pollDelay, TaskDao taskDao, HazelcastInstance hazelcastInstance, long workerTimeoutMilliseconds) {

        this.processService = new HzProcessService(hazelcastInstance, "Process");
        this.taskService = new GeneralTaskService(taskDao, workerTimeoutMilliseconds);
        StorageFactory storageFactory = new DefaultStorageFactory(hazelcastInstance, "storageName", 100l);
        QueueFactory queueFactory = new DefaultQueueFactory(hazelcastInstance, storageFactory);
        this.queueService = new HzQueueService(queueFactory, hazelcastInstance, "q:", 5000, pollDelay);

        this.graphDao = new HzGraphDao(hazelcastInstance);
        this.dependencyService = new GeneralDependencyService(graphDao);
        this.configService = new HzConfigService(hazelcastInstance, "actorPreferencesMap");
        this.interruptedTasksService = new HzInterruptedTasksService(hazelcastInstance, "BrokenProcess", taskService, queueService);
    }

    @Override
    public ProcessService getProcessService() {
        return processService;
    }

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    public QueueService getQueueService() {
        return queueService;
    }

    @Override
    public DependencyService getDependencyService() {
        return dependencyService;
    }

    @Override
    public ConfigService getConfigService() {
        return configService;
    }

    @Override
    public InterruptedTasksService getInterruptedTasksService() {
        return interruptedTasksService;
    }

    @Override
    public GarbageCollectorService getGarbageCollectorService() {
        return garbageCollectorService;
    }
}
