package ru.taskurotta.service.hz;

import com.hazelcast.core.HazelcastInstance;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.GeneralDependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.hz.config.HzConfigService;
import ru.taskurotta.service.hz.dependency.HzGraphDao;
import ru.taskurotta.service.hz.queue.HzMongoQueueService;
import ru.taskurotta.service.hz.storage.HzProcessService;
import ru.taskurotta.service.process.BrokenProcessService;
import ru.taskurotta.service.process.MemoryBrokenProcessService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.GeneralTaskService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.service.storage.TaskDao;

import java.util.concurrent.TimeUnit;

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
    private BrokenProcessService brokenProcessService;
    private GarbageCollectorService garbageCollectorService;

    public HzServiceBundle(int pollDelay, TaskDao taskDao, HazelcastInstance hazelcastInstance) {

        this.processService = new HzProcessService(hazelcastInstance, "Process");

        this.taskService = new GeneralTaskService(taskDao);

        this.queueService = new HzMongoQueueService(pollDelay, TimeUnit.SECONDS, hazelcastInstance);

        this.graphDao = new HzGraphDao(hazelcastInstance);
        this.dependencyService = new GeneralDependencyService(graphDao);
        this.configService = new HzConfigService(hazelcastInstance, "actorPreferencesMap");
        this.brokenProcessService = new MemoryBrokenProcessService();
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
    public BrokenProcessService getBrokenProcessService() {
        return brokenProcessService;
    }

    @Override
    public GarbageCollectorService getGarbageCollectorService() {
        return garbageCollectorService;
    }
}
