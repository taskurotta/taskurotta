package ru.taskurotta.service.hz.server;

import com.hazelcast.core.PartitionAware;
import com.hazelcast.spring.context.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.taskurotta.service.hz.TaskKey;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Callable task for processing taskDecisions
 */
@SpringAware
public class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);

    TaskKey taskKey;
    HzTaskServer taskServer;

    public ProcessDecisionUnitOfWork() {
    }

    public ProcessDecisionUnitOfWork(TaskKey TaskKey) {
        this.taskKey = TaskKey;
    }

    @Autowired
    public void setTaskServer(HzTaskServer taskServer) {
        this.taskServer = taskServer;
    }

    @Override
    public Object call() throws Exception {
        try {
            HzTaskServer.lockAndProcessDecision(taskKey, taskServer);
        } catch (RuntimeException ex) {
            logger.error("Can not process task decision", ex);
            throw ex;
        }

        return null;
    }

    @Override
    public Object getPartitionKey() {
        return taskKey.getProcessId();
    }

    public TaskKey getTaskKey() {
        return taskKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessDecisionUnitOfWork that = (ProcessDecisionUnitOfWork) o;

        if (taskKey != null ? !taskKey.equals(that.taskKey) : that.taskKey != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return taskKey != null ? taskKey.hashCode() : 0;
    }
}
