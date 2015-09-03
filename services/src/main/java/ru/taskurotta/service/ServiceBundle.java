package ru.taskurotta.service;

import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:28 PM
 */
public interface ServiceBundle {

    ProcessService getProcessService();

    TaskService getTaskService();

    QueueService getQueueService();

    DependencyService getDependencyService();

    ConfigService getConfigService();

    GraphDao getGraphDao();

    InterruptedTasksService getInterruptedTasksService();

    GarbageCollectorService getGarbageCollectorService();

    QueueService recreateQueueService();
}
