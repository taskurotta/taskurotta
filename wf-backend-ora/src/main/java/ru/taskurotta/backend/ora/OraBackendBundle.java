package ru.taskurotta.backend.ora;

import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.MemoryDependencyBackend;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.queue.OraQueueBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.GeneralTaskBackend;
import ru.taskurotta.backend.storage.MemoryProcessBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.backend.storage.TaskDao;

import javax.sql.DataSource;


public class OraBackendBundle implements BackendBundle {


    private ProcessBackend processBackend;
    private TaskBackend taskBackend;
    private QueueBackend queueBackend;
    private DependencyBackend dependencyBackend;
    private ConfigBackend configBackend;

    public OraBackendBundle(DataSource dataSource, TaskDao taskDao) {
        this.processBackend = new MemoryProcessBackend();
        this.taskBackend = new GeneralTaskBackend(taskDao);
        this.queueBackend = new OraQueueBackend(dataSource);
        this.dependencyBackend = new MemoryDependencyBackend();
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
