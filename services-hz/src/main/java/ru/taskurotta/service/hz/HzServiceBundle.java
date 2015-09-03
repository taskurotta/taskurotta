package ru.taskurotta.service.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.hazelcast.queue.delay.DefaultQueueFactory;
import ru.taskurotta.hazelcast.queue.delay.DefaultStorageFactory;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;
import ru.taskurotta.hazelcast.queue.delay.StorageFactory;
import ru.taskurotta.hazelcast.util.ConfigUtil;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.GeneralDependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.hz.config.HzConfigService;
import ru.taskurotta.service.hz.dependency.HzGraphDao;
import ru.taskurotta.service.hz.gc.HzGarbageCollectorService;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.hz.storage.HzInterruptedTasksService;
import ru.taskurotta.service.hz.storage.HzProcessService;
import ru.taskurotta.service.hz.storage.HzTaskDao;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.GeneralTaskService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.service.storage.TaskService;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class HzServiceBundle implements ServiceBundle {

    private long pollDelay;
    private HazelcastInstance hazelcastInstance;
    private QueueFactory queueFactory;

    private ProcessService processService;
    private TaskService taskService;
    private QueueService queueService;
    private DependencyService dependencyService;
    private ConfigService configService;
    private GraphDao graphDao;
    private InterruptedTasksService interruptedTasksService;
    private GarbageCollectorService garbageCollectorService;

    private long workerTimeoutMilliseconds = 30000l;

    public HzServiceBundle(long pollDelay) {
        this.pollDelay = pollDelay;
        this.hazelcastInstance = ConfigUtil.newInstanceWithoutMulticast();
        TaskDao taskDao = new HzTaskDao(hazelcastInstance, "Task", "TaskDecision");

        this.processService = new HzProcessService(hazelcastInstance, "Process");
        this.taskService = new GeneralTaskService(taskDao, workerTimeoutMilliseconds);
        StorageFactory storageFactory = new DefaultStorageFactory(hazelcastInstance, "storageName", 100l);
        this.queueFactory = new DefaultQueueFactory(hazelcastInstance, storageFactory);
        this.queueService = new HzQueueService(queueFactory, hazelcastInstance, "q:", 5000, pollDelay);

        this.graphDao = new HzGraphDao(hazelcastInstance);
        this.dependencyService = new GeneralDependencyService(graphDao);
        this.configService = new HzConfigService(hazelcastInstance, "actorPreferencesMap");
        this.interruptedTasksService = new HzInterruptedTasksService(hazelcastInstance, "BrokenProcess");
        this.garbageCollectorService = new HzGarbageCollectorService(processService, graphDao, taskDao, queueFactory,
                "GarbageCollector", 1, true);
    }

    public HzServiceBundle(long pollDelay, TaskDao taskDao) {
        this(pollDelay, taskDao, ConfigUtil.newInstanceWithoutMulticast());
    }

    public HzServiceBundle(long pollDelay, TaskDao taskDao, HazelcastInstance hazelcastInstance) {
        this.pollDelay = pollDelay;
        this.hazelcastInstance = hazelcastInstance;

        this.processService = new HzProcessService(hazelcastInstance, "Process");
        this.taskService = new GeneralTaskService(taskDao, workerTimeoutMilliseconds);
        StorageFactory storageFactory = new DefaultStorageFactory(hazelcastInstance, "storageName", 100l);
        this.queueFactory = new DefaultQueueFactory(hazelcastInstance, storageFactory);
        this.queueService = new HzQueueService(queueFactory, hazelcastInstance, "q:", 5000, pollDelay);

        this.graphDao = new HzGraphDao(hazelcastInstance);
        this.dependencyService = new GeneralDependencyService(graphDao);
        this.configService = new HzConfigService(hazelcastInstance, "actorPreferencesMap");
        this.interruptedTasksService = new HzInterruptedTasksService(hazelcastInstance, "BrokenProcess");
        this.garbageCollectorService = new HzGarbageCollectorService(processService, graphDao, taskDao, queueFactory,
                "GarbageCollector", 1, true);
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
    public GraphDao getGraphDao() {
        return graphDao;
    }

    @Override
    public InterruptedTasksService getInterruptedTasksService() {
        return interruptedTasksService;
    }

    @Override
    public GarbageCollectorService getGarbageCollectorService() {
        return garbageCollectorService;
    }

    @Override
    public QueueService recreateQueueService() {
        this.queueService = new HzQueueService(queueFactory, hazelcastInstance, "q:", 5000, pollDelay);
        return queueService;
    }
}
