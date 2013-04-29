package ru.taskurotta.backend.ora;

import javax.sql.DataSource;

import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.GeneralDependencyBackend;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.ora.queue.OraQueueBackend;
import ru.taskurotta.backend.ora.storage.OraProcessBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;


public class OraBackendBundle implements BackendBundle {


    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;

    public OraBackendBundle(DataSource dataSource, TaskDao taskDao, CheckpointService checkpointService, GraphDao graphDao) {
        this.processBackend = new OraProcessBackend(dataSource, checkpointService);
        this.taskBackend = new GeneralTaskBackend(taskDao, checkpointService);
        this.queueBackend = new OraQueueBackend(dataSource);
        this.dependencyBackend = new GeneralDependencyBackend(graphDao, 100000);
        this.configBackend = new MemoryConfigBackend();
    }

    public ConfigBackend getConfigBackend() {
        return configBackend;
    }

    public DependencyBackend getDependencyBackend() {
        return dependencyBackend;
    }

    public ProcessBackend getProcessBackend() {
        return processBackend;
    }

    public QueueBackend getQueueBackend() {
        return queueBackend;
    }

    public TaskBackend getTaskBackend() {
        return taskBackend;
    }
}
