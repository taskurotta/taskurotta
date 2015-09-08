package ru.taskurotta.service.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.hazelcast.queue.CachedQueue;
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
import ru.taskurotta.util.ActorUtils;

import java.util.Collection;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:30 PM
 */
public class HzServiceBundle implements ServiceBundle {

    private long pollDelay;
    private TaskDao taskDao;
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

    public HzServiceBundle(long pollDelay) {
        this.pollDelay = pollDelay;
        this.hazelcastInstance = HzInstanceFactory.createHzInstanceForTest();
        this.taskDao = new HzTaskDao(hazelcastInstance, "Task", "TaskDecision");

        init();
    }

    public HzServiceBundle(long pollDelay, TaskDao taskDao, HazelcastInstance hazelcastInstance) {
        this.pollDelay = pollDelay;
        this.taskDao = taskDao;
        this.hazelcastInstance = hazelcastInstance;

        init();
    }

    private void init() {
        this.processService = new HzProcessService(hazelcastInstance, "Process");
        this.taskService = new GeneralTaskService(taskDao, 30000l);
        StorageFactory storageFactory = new DefaultStorageFactory(hazelcastInstance, "storageName", 100l);
        this.queueFactory = new DefaultQueueFactory(hazelcastInstance, storageFactory);
        this.queueService = newQueueService();
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

    public QueueService newQueueService() {
        String queueNamePrefix = "q:";
        if (queueService != null) {
            String cachedQueueClassName = CachedQueue.class.getName();
            Collection<String> queueNames = queueService.getQueueNames();
            for (String queueName : queueNames) {
                String qName = ActorUtils.toPrefixed(queueName, queueNamePrefix);
                hazelcastInstance.getDistributedObject(cachedQueueClassName, qName).destroy();
            }
        }

        this.queueService = new HzQueueService(queueFactory, hazelcastInstance, queueNamePrefix, 5000, pollDelay);
        return queueService;
    }

}
