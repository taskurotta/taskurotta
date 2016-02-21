package ru.taskurotta.server.test;

import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.transport.model.TaskContainer;

/**
 */
public class ActorEngine {

    TaskServer taskServer;
    ObjectFactory objectFactory  = new ObjectFactory();

    public ActorEngine(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

    public void startProcess(MockStartTask task) {
        startProcess(task, 1);
    }

    public void startProcess(MockStartTask task, int howManyTimes) {
        task.setType(TaskType.DECIDER_START);

        for (int i = 0; i < howManyTimes; i++) {
            task.generateNewIds();
            taskServer.startProcess(objectFactory.dumpTask(task));
        }
    }

    public void executeActor(MockDecision decision) {
        executeActor(decision, 1);
    }

    public void executeActor(MockDecision decision, int howManyTimes) {

        for (int i = 0; i < howManyTimes; i++) {

            TaskContainer taskContainer = taskServer.poll(decision.getActorDefinition());
            if (taskContainer == null) {
                throw new IllegalStateException("no tasks for actor " + decision.getActorDefinition());
            }

            decision.correspondsTo(taskContainer);
            taskServer.release(objectFactory.dumpResult(decision, decision.getActorId()));

        }
    }
}
