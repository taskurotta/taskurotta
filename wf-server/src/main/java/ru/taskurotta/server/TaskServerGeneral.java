package ru.taskurotta.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.model.TaskObject;
import ru.taskurotta.server.model.TaskStateObject;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.ResultContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 1:09 PM
 */
public class TaskServerGeneral implements TaskServer {

    private final static Logger log = LoggerFactory.getLogger(TaskServerGeneral.class);

    private static final String ACTOR_ID = "InMemoryActor";
    private static final int queueCapacity = 1000;

    private TaskDao taskDao;

    public TaskServerGeneral(TaskDao taskDao) {

        this.taskDao = taskDao;
    }

    @Override
    public void startProcess(TaskContainer task) {
        addTask(task, null, null, false);
    }

    @Override
    public TaskContainer pull(ActorDefinition actorDefinition) {
        return pullInternal(actorDefinition);
    }

    @Override
    public void release(ResultContainer taskResult) {
        releaseInternal(taskResult);
    }

    private TaskContainer pullInternal(ActorDefinition actorDefinition) {

        TaskObject task = taskDao.pull(actorDefinition);

        ArgContainer[] args = task.getArgs();

        if (args != null) {

            for (ArgContainer arg : args) {
                if (arg.isPromise() && arg.getJSONValue() == null) {
                    ArgContainer value = taskDao.getTaskValue((arg).getTaskId());
                    arg.setJSONValue(value.getJSONValue());
                    arg.setClassName(value.getClassName());
                    arg.setReady(true);
                }
            }

        }

        return task;
    }

    protected TaskObject getTaskById(UUID taskId) {
        return taskDao.findById(taskId);
    }


    public void addTask(TaskContainer task, UUID parentTaskId, List<UUID> waitingId, boolean isDependTask) {

        log.debug("Add task = [{}]", task);

        TaskObject taskObj = new TaskObject(task);

        taskObj.setParentId(parentTaskId);
        taskObj.setWaitingId(waitingId);
        taskObj.setDependTask(isDependTask);

        log.debug("Add task [{}]. waitingId = [{}]", taskObj.getTaskId(), waitingId);

        TaskStateObject taskStateObj = new TaskStateObject(ACTOR_ID, TaskStateObject.STATE.wait, System.currentTimeMillis());
        taskObj.setState(taskStateObj);


        int countdown = 0;

        if (task.getArgs() != null) {

            for (ArgContainer arg : task.getArgs()) {

                if (arg != null && arg.isPromise()) {
                    if (!arg.isReady()) {
                        countdown++;
                    }
                }
            }
        }

        log.debug("Add task id [{}]. countdown = {}", task.getTaskId(), countdown);

        if (countdown != 0) {
            taskObj.setCountdown(countdown);
        }

        taskDao.add(taskObj);
    }

    private void releaseInternal(ResultContainer taskResult) {

        taskDao.logTaskResult(taskResult);

        processReleasedTask(taskResult);

        taskDao.unlogTaskResult(taskResult.getTaskId());
    }


    private void processReleasedTask(ResultContainer taskResult) {


        // 1. Изменить state задачи через Dao
        // 2. Подабовлять все задачи в хранилище через Dao

        // set to Done\Depend state first! Cause task can be in Depend state and its dependent task can finish before.

        UUID taskId = taskResult.getTaskId();
        ArgContainer value = taskResult.getValue();

        UUID dependTaskId = null;
        TaskStateObject.STATE currentState = TaskStateObject.STATE.done;

        // calculate new state
        if (value != null && value.isPromise()) {

            log.debug("Task id [{}] has promise isReady = {} state", value.getTaskId(), value.isReady());

            if (!value.isReady()) {
                currentState = TaskStateObject.STATE.depend;
                dependTaskId = value.getTaskId();
            }
        }

        TaskStateObject taskStateObj = new TaskStateObject(ACTOR_ID, currentState, System.currentTimeMillis());

        taskDao.saveTaskValue(taskId, value, taskStateObj);


        // - registration  of all new tasks
        TaskContainer[] childTasks = taskResult.getTasks();
        if (childTasks != null) {

            // iterate in reverse order cause independent task can be released too early.
            for (int i = childTasks.length - 1; i >= 0; i--) {

                TaskContainer task = childTasks[i];

                List<UUID> waitingId = null;

                for (TaskContainer otherTask : childTasks) {
                    ArgContainer args[] = otherTask.getArgs();
                    if (args != null) {
                        for (ArgContainer arg : args) {
                            if (arg.isPromise()) {
                                if (arg.getTaskId().equals(task.getTaskId())) {
                                    if (waitingId == null) {
                                        waitingId = new ArrayList<UUID>();
                                    }

                                    waitingId.add(otherTask.getTaskId());
                                }
                            }
                        }
                    }
                }

                addTask(task, taskId, waitingId, dependTaskId != null && dependTaskId.equals(task.getTaskId()));
            }
        }

        // recursion start compileDoneTask(taskMemory)
        if (currentState == TaskStateObject.STATE.done) {

            TaskObject taskObj = taskDao.findById(taskId);
            resolveDependency(taskObj);
        }

    }

    private void resolveDependency(TaskObject taskObj) {

        log.debug("resolveDependency taskId = [{}]", taskObj.getTaskId());

        // analise all waiting tasks
        // - decrement task.countdown for all tasks in taskMemory.waitingId list
        List<UUID> waitingUUIDs = taskObj.getWaitingId();
        if (waitingUUIDs != null && !waitingUUIDs.isEmpty()) {
            for (UUID uuid : waitingUUIDs) {

                log.debug("task for countdown decrement [{}]", uuid);

                taskDao.decrementCountdown(uuid);

            }
        }

        // analise parent and its "depend" state
        if (taskObj.isDependTask()) {

            log.debug("task has parent task [{}]", taskObj.getParentId());

            TaskObject parentTask = taskDao.findById(taskObj.getParentId());

            log.debug("parent task state is [{}]", parentTask.getState());

            ArgContainer taskValue = taskObj.getValue();

            // - prepare new state
            TaskStateObject taskStateObj = new TaskStateObject(ACTOR_ID, TaskStateObject.STATE.done, System.currentTimeMillis());


            taskDao.saveTaskValue(taskObj.getParentId(), taskValue, taskStateObj);

            // decrement countdown on waiting tasks
            // check its parent and its "depend" state"
            // set to done state
            //compileDoneTask(taskMemory)
            resolveDependency(parentTask);
        }

    }

}
