package ru.taskurotta;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.test.TestTasks;

import java.util.UUID;

/**
 * User: romario
 * Date: 1/11/13
 * Time: 2:43 PM
 */
public class WEStartWFTest {
    protected static final Logger logger = LoggerFactory.getLogger(WEStartWFTest.class);

    // worker client
    // =================

    @Worker
    public static interface SimpleWorker {
        public int max(int a, int b);
    }

    @WorkerClient(worker = SimpleWorker.class)
    public static interface SimpleWorkerClient {
        public Promise<Integer> max(int a, int b);
    }


    // decider client
    // =================

    @Decider
    public static interface SimpleSubDecider {

        @Execute
        public void startIt(String str);
    }

    @DeciderClient(decider = SimpleSubDecider.class)
    public static interface SimpleSubDeciderClient {

        public Promise<Void> startIt(String str);
    }


    // decider
    // ==================

    @Decider
    public static interface SimpleDecider {

        @Execute
        public void start();
    }


    public static class SimpleDeciderImpl implements SimpleDecider {

        public SimpleDeciderImpl async;

        protected SimpleWorkerClient simpleWorkerClient;

        protected SimpleSubDeciderClient simpleSubDeciderClient;

        @Override
        public void start() {

            Promise<Integer> max = simpleWorkerClient.max(10, 55);
            async.proceed(max);
        }

        @Asynchronous
        public void proceed(Promise<Integer> pValue) {
            int value = pValue.get();
            if (value > 30) {
                simpleSubDeciderClient.startIt("abc");
            } else {
                simpleSubDeciderClient.startIt("cBa");
            }
        }
    }


    @Test
    public void startSimpleWF() {

        RuntimeProvider runtimeProvider = RuntimeProviderManager.getRuntimeProvider();
        SimpleDeciderImpl simpleDeciderImpl = new SimpleDeciderImpl();
        simpleDeciderImpl.async = ProxyFactory.getAsynchronousClient(SimpleDeciderImpl.class);
        simpleDeciderImpl.simpleSubDeciderClient = ProxyFactory.getDeciderClient(SimpleSubDeciderClient.class);
        simpleDeciderImpl.simpleWorkerClient = ProxyFactory.getWorkerClient(SimpleWorkerClient.class);

        RuntimeProcessor runtimeProcessor = runtimeProvider.getRuntimeProcessor(simpleDeciderImpl);

        TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_START, SimpleDecider.class.getName(), "1.0", "start");
        Task task = TestTasks.newInstance(UUID.randomUUID(), taskTarget, null);

        TaskDecision taskDecision = runtimeProcessor.execute(task, null);

        // should be one task: TestWorker.doId("Drag and Drop!");
        Task[] taskList = taskDecision.getTasks();

        for (Task taskInList : taskList) {
            System.out.println("Task: " + taskInList);
        }

//        Task patternTask = new TaskImpl(
//                newTask.getId(),
//                new TaskTargetImpl(TaskType.WORKER, TestWorker.class.getName(), "1.0", "doIt"),
//                new Object[]{testWorkStr});

    }

}
