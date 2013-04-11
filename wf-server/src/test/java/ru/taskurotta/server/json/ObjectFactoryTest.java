package ru.taskurotta.server.json;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.test.TestTasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 4:46 PM
 */
public class ObjectFactoryTest {
    protected final static Logger log = LoggerFactory.getLogger(ObjectFactoryTest.class);

    private ObjectFactory objectFactory;

    @Before
    public void setUp() {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void argContainerBoolean() {
        Boolean arg = Boolean.FALSE;
        testInternal(arg);
    }

    //FixMe: it fails
    @org.junit.Ignore
    @Test
    public void argContainerArrayInt() {
        int[] arg = new int[10];
        testInternal(arg);
    }

    @Test
    public void argContainerListInt() {
        List<Integer> arg = new ArrayList<Integer>(10);
        for (int i = 0; i < 10; i++) {
            arg.add(i);
        }

        testInternal(arg);
    }

    @Test
    public void argContainerLinkedListInt() {
        List<Integer> arg = new LinkedList<Integer>();
        for (int i = 0; i < 10; i++) {
            arg.add(i);
        }

        testInternal(arg);
    }

    //FixMe: it fails
    @org.junit.Ignore
    @Test
    public void argContainerMapInt() {
        Map<Integer, Object> arg = new HashMap<Integer, Object>();
        Promise payload = Promise.asPromise(true);
        for (int i = 0; i < 10; i++) {
            arg.put(i, payload);
        }

        testInternal(arg);
    }

    @Test
    public void argContainerReadyPromise() {
        Promise arg = Promise.asPromise(Boolean.TRUE);
        testInternal(arg);
    }

    @Test
    public void argContainerNotReadyPromise() {
        Promise arg = Promise.createInstance(UUID.randomUUID());
        testInternal(arg);
    }

    private void testInternal(Object arg) {
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        Object newArg = objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg, newArg);
    }

    @Test
    public void taskSimple() {
        Task task =
                TestTasks.newInstance(
                        UUID.randomUUID(),
                        new TaskTargetImpl(TaskType.DECIDER_START, "ru.example.Decider", "1.0", "start"),
                        new Object[]{true, "Hello!", 10});


        TaskContainer taskContainer = objectFactory.dumpTask(task);

        Task newTask = objectFactory.parseTask(taskContainer);

        assertEquals(task, newTask);

    }

    // TODO
    @Test
    public void resultContainerSimple() {
//        Task task = new Task[]{
//                TestTasks.newInstance(
//                        UUID.randomUUID(),
//                        new TaskTargetImpl(TaskType.DECIDER_START, "ru.example.Decider", "1.0", "start"),
//                        new Object[]{true, "Hello!", 10})
//        };
//
//        TaskDecision taskDecision = new TaskDecisionImpl(UUID.randomUUID(), UUID.randomUUID(), Boolean.TRUE, tasks);
//
//        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision);
//
//        System.err.println("decisionContainer = " + decisionContainer);
//
//        TaskDecision newTaskDecision = objectFactory.parseResult(decisionContainer);
//
//        System.err.println("newTaskDecision = " + newTaskDecision);
//
//        assertEquals(taskDecision, newTaskDecision);

    }

    // TODO: test dupm of DecisionContainer
}
