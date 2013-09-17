package ru.taskurotta.dropwizard.server.pooling;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.concurrent.Callable;


/**
 * Represents TaskServer interface methods as Callable tasks
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
