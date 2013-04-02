package ru.taskurotta.server.json;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.test.TestTasks;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 4:46 PM
 */
public class ObjectFactoryTest {

    private ObjectFactory objectFactory;

    @Before
    public void setUp() {
        objectFactory = new ObjectFactory();
    }


    @Test
    public void argContainerBoolean() {

        Boolean arg = Boolean.FALSE;

        ArgContainer argContainer = objectFactory.dumpArg(arg);

        System.err.println("argContainer = " + argContainer);

        Object newArg = objectFactory.parseArg(argContainer);

        System.err.println("newArg = " + newArg);

        assertEquals(arg, newArg);
    }

    @Test
    public void argContainerReadyPromise() {

        Promise arg = Promise.asPromise(Boolean.TRUE);

        ArgContainer argContainer = objectFactory.dumpArg(arg);

        System.err.println("argContainer = " + argContainer);

        Object newArg = objectFactory.parseArg(argContainer);

        System.err.println("newArg = " + newArg);

        assertEquals(arg, newArg);
    }

    @Test
    public void argContainerNotReadyPromise() {

        Promise arg = Promise.createInstance(UUID.randomUUID());

        ArgContainer argContainer = objectFactory.dumpArg(arg);

        System.err.println("argContainer = " + argContainer);

        Object newArg = objectFactory.parseArg(argContainer);

        System.err.println("newArg = " + newArg);

        assertEquals(arg, newArg);
    }


    @Test
    public void resultContainerSimple() {
        Task[] tasks = new Task[]{
                TestTasks.newInstance(
                        new TaskTargetImpl(TaskType.DECIDER_START, "ru.example.Decider", "1.0", "start"),
                        new Object[]{true, "Hello!", 10})
        };

        TaskDecision taskDecision = new TaskDecisionImpl(UUID.randomUUID(), Boolean.TRUE, tasks);

        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision);

        System.err.println("decisionContainer = " + decisionContainer);

        TaskDecision newTaskDecision = objectFactory.parseResult(decisionContainer);

        System.err.println("newTaskDecision = " + newTaskDecision);

        assertEquals(taskDecision, newTaskDecision);

    }
}
