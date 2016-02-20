package ru.taskurotta.server.test;

import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;

/**
 */
public class ActorEngine {

    TaskServer taskServer;
    ObjectFactory objectFactory  = new ObjectFactory();

    public ActorEngine(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

    public void startProcess(MockTask task) {
        startProcess(task, 1);
    }

    public void startProcess(MockTask task, int howManyTimes) {
        task.setType(TaskType.DECIDER_START);

        taskServer.startProcess(objectFactory.dumpTask(task));
    }

    public void executeActor(MockDecision decision) {
        executeActor(decision, 1);
    }

    public void executeActor(MockDecision decision, int howManyTimes) {
    }
}
