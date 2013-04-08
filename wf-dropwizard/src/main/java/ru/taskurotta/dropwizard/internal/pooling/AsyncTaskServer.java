package ru.taskurotta.dropwizard.internal.pooling;

import java.util.concurrent.Callable;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;


/**
 * Represents TaskServer interface methods as Callable tasks
 *
 */
public class AsyncTaskServer {

    private TaskServer taskServer;

    public AsyncTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

    public Callable<TaskContainer> callPull(final ActorDefinition actorDefinition) {
        return new Callable<TaskContainer>() {
            @Override
            public TaskContainer call() throws Exception {
                return taskServer.poll(actorDefinition);
            }
        };
    }

    public Callable<Boolean> callRelease(final DecisionContainer decisionContainer) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                taskServer.release(decisionContainer);
                return Boolean.TRUE;
            }
        };
    }

    public Callable<Boolean> callStartTask(final TaskContainer taskContainer) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                taskServer.startProcess(taskContainer);
                return Boolean.TRUE;
            }
        };
    }
}
