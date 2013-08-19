package ru.taskurotta.backend.hz.dependency;

import com.hazelcast.core.PartitionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 14.08.13
 * Time: 13:09
 */
public class StartProcessTask implements Callable, PartitionAware, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(StartProcessTask.class);

    private TaskContainer taskContainer;

    public StartProcessTask(TaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    @Override
    public Object call() throws Exception {

        logger.debug("Try to start process from task container [{}]", taskContainer);

        HzDependencyBackend dependencyBackend = HzDependencyBackend.getInstance();
        dependencyBackend.startProcess(taskContainer);

        logger.info("Start process [{}] from start task [{}]", taskContainer.getProcessId(), taskContainer.getTaskId());

        return null;
    }

    @Override
    public Object getPartitionKey() {
        return taskContainer.getProcessId();
    }
}
