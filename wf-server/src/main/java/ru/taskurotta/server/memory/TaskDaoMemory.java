package ru.taskurotta.server.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.server.TaskDao;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.server.model.TaskStateObject;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.ResultContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 12:59 PM
 */
public class TaskDaoMemory implements TaskDao {


    private final static Logger log = LoggerFactory.getLogger(TaskDaoMemory.class);

    private static final String ACTOR_ID = "InMemoryActor";
    private static final int queueCapacity = 1000;

    private Map<String, BlockingQueue<TaskObject>> queues = new ConcurrentHashMap<String, BlockingQueue<TaskObject>>();
    private Map<UUID, TaskObject> taskMap = new ConcurrentHashMap<UUID, TaskObject>();
    private Map<UUID, AtomicInteger> atomicCountdownMap = new ConcurrentHashMap<UUID, AtomicInteger>();

    public TaskDaoMemory() {

        if (log.isTraceEnabled()) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        log.trace("#### queues.size() = [{}] ", queues.size());

                        for (String queueId : queues.keySet()) {
                            BlockingQueue<TaskObject> queue = queues.get(queueId);
                            log.trace("#### [{}] size = [{}]", queueId, queue.size());

                        }

                        log.trace("#### taskMap.size() = [{}]" + taskMap.size());
                        for (UUID taskId : taskMap.keySet()) {
                            log.trace("#### [{}]", taskMap.get(taskId));
                        }

                    }


                }
            });

            thread.start();
        }
    }


    /**
     * @param taskId
     * @return
     */
    @Override
    public ArgContainer getTaskValue(UUID taskId) {
        TaskObject taskObj = taskMap.get(taskId);

        return taskObj.getValue();
    }


    @Override
    public void decrementCountdown(UUID taskId) {

        AtomicInteger atomicCountdown = atomicCountdownMap.get(taskId);

        int taskCountdown = atomicCountdown.decrementAndGet();
        log.debug("task [{}] countdown value is [{}] after decrement", taskId, taskCountdown);

        TaskObject taskObj = taskMap.get(taskId);
        taskObj.setCountdown(taskCountdown);

        // push task to work (to queue)
        if (taskCountdown == 0) {
            addTaskToQueue(taskMap.get(taskId));
        }

    }


    @Override
    public void logTaskResult(ResultContainer taskResult) {
        // nothing to do, because memory implementation doesn't need recovery functionality
    }

    @Override
    public void unlogTaskResult(UUID taskId) {
        // nothing to do, because memory implementation doesn't need recovery functionality
    }

    @Override
    public void saveTaskValue(UUID taskId, ArgContainer value, TaskStateObject taskStateMemory) {

        TaskObject taskObj = taskMap.get(taskId);
        taskObj.setValue(value);
        taskObj.setState(taskStateMemory);
    }


    @Override
    public TaskObject findById(UUID taskId) {
        return taskMap.get(taskId);
    }

    @Override
    public void add(TaskObject taskObj) {
        taskMap.put(taskObj.getTaskId(), taskObj);

        int countdown = taskObj.getCountdown();

        if (countdown == 0) {
            // just put task to queue if it has no Promise.
            addTaskToQueue(taskObj);
        } else {
            AtomicInteger atomicCountdown = new AtomicInteger(countdown);
            atomicCountdownMap.put(taskObj.getTaskId(), atomicCountdown);
        }

    }

    @Override
    public TaskObject pull(ActorDefinition actorDefinition) {

        String queueId = getQueueName(actorDefinition.getName(), actorDefinition.getVersion());

        BlockingQueue<TaskObject> queue = getQueue(queueId);

        TaskObject task = null;
        try {
            task = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            // TODO: General policy about exceptions
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        setProgressSate(task);
        return task;
    }


    private void setProgressSate(TaskObject task) {

        TaskStateObject taskState = new TaskStateObject(ACTOR_ID, TaskStateObject.STATE.process, System.currentTimeMillis());
        task.setState(taskState);
    }


    private String getQueueName(String actorDefinitionName, String actorDefinitionVersion) {
        return actorDefinitionName + '#' + actorDefinitionVersion;
    }

    private BlockingQueue<TaskObject> getQueue(String queueName) {

        BlockingQueue<TaskObject> queue = queues.get(queueName);
        if (queue == null) {
            synchronized (this) {
                queue = new ArrayBlockingQueue<TaskObject>(queueCapacity);
                queues.put(queueName, queue);
            }
        }

        return queue;
    }

    private void addTaskToQueue(TaskObject taskMemory) {

        TaskTarget taskTarget = taskMemory.getTarget();

        String queueName = getQueueName(taskTarget.getName(), taskTarget.getVersion());
        Queue<TaskObject> queue = getQueue(queueName);
        queue.add(taskMemory);
    }
}
