package ru.taskurotta.dropwizard.server.pooling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 * TaskServer interface implementation with task execution via ExecutorService,
 * thus allowing additional control over threads
 */
public class ThreadPooledTaskServer implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPooledTaskServer.class);

    private AsyncTaskServer asyncTaskServer;

    private ExecutorService executorService;

    private long timeout = -1l;

    /**
     * @param taskServer TaskServer implementation to be decorated with
     */
    public ThreadPooledTaskServer(TaskServer taskServer) {
        this.asyncTaskServer = new AsyncTaskServer(taskServer);
    }

    @Override
    public void startProcess(TaskContainer task) {
        try {
            Future<Boolean> futureResult = executorService.submit(asyncTaskServer.callStartTask(task));
            if(timeout>0) {
                futureResult.get(timeout, TimeUnit.MILLISECONDS);
            } else{
                futureResult.get();
            }
        } catch (TimeoutException e) {
            logger.debug("startProcess(["+task+"]) timed out", e);
        } catch (Exception ex) {
            logger.error("startProcess(["+task+"]) failed", ex);
        }
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        TaskContainer result = null;
        try {
            Future<TaskContainer> futureResult = executorService.submit(asyncTaskServer.callPull(actorDefinition));
            if(timeout>0) {
                result = futureResult.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                result = futureResult.get();
            }
        } catch (TimeoutException e) {
            logger.debug("poll(["+actorDefinition+"]) timed out", e);
        } catch (Exception e) {
            logger.error("poll(["+actorDefinition+"]) failed", e);
        }
        return result;
    }

    @Override
    public void release(DecisionContainer decisionContainer) {

        try {
            Future<Boolean> futureResult = executorService.submit(asyncTaskServer.callRelease(decisionContainer));
            if(timeout > 0) {
                futureResult.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                futureResult.get();
            }
        } catch (TimeoutException e) {
            logger.debug("release(["+decisionContainer+"]) timed out", e);
        } catch (Exception e) {
            logger.error("release(["+decisionContainer+"]) failed", e);
        }
    }

    @Required
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
