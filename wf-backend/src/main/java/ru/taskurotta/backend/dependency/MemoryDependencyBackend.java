package ru.taskurotta.backend.dependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.dependency.model.TaskDependency;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.ArgType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 4:39 PM
 */
public class MemoryDependencyBackend implements DependencyBackend {

    private final static Logger logger = LoggerFactory.getLogger(MemoryDependencyBackend.class);

    private Map<UUID, TaskDependency> id2depMap = new ConcurrentHashMap<UUID, TaskDependency>();

    @Override
    public DependencyDecision applyDecision(DecisionContainer taskDecision) {

        DependencyDecision dependencyDecision = new DependencyDecision();

        UUID taskId = taskDecision.getTaskId();
        ArgContainer value = taskDecision.getValue();

        UUID dependTaskId = null;

        logger.debug("analiseDecision() taskId = [{}]", taskId);

        // calculate new state
        if (value != null && value.isPromise()) {

            logger.debug("analiseDecision() taskId = [{}] has promise value = {}", taskDecision.getTaskId(), value);

            if (!value.isReady()) {
                dependTaskId = value.getTaskId();
            }
        }


        // - registration of all new tasks
        TaskContainer[] childTasks = taskDecision.getTasks();
        if (childTasks != null) {

            for (TaskContainer childTask : childTasks) {

                UUID childTaskId = childTask.getTaskId();

                List<UUID> thatWaitThis = null;

                // go throw all tasks and find Promise argument with childTaskId
                for (TaskContainer otherChildTask : childTasks) {

                    // skip the same task
                    if (otherChildTask.getTaskId().equals(childTaskId)) {
                        continue;
                    }

                    ArgContainer args[] = otherChildTask.getArgs();

                    TaskOptionsContainer taskOptionsContainer = otherChildTask.getOptions();
                    ArgType[] argTypes = taskOptionsContainer != null ? taskOptionsContainer.getArgTypes() : null;

                    if (args != null) {
                        for (int j = 0; j < args.length; j++) {
                            ArgContainer arg = args[j];

                            boolean isPromise = arg.isPromise();

                            // skip not promises or resolved promises
                            if (!isPromise || (isPromise && arg.isReady())) {
                                continue;
                            }

                            // skip @NoWait promises
                            if (argTypes != null) {
                                if (ArgType.NO_WAIT.equals(argTypes[j])) {
                                    continue;
                                }
                            }

                            UUID otherChildTaskTaskId = otherChildTask.getTaskId();

                            if (arg.getTaskId().equals(childTaskId)) {
                                if (thatWaitThis == null) {
                                    thatWaitThis = new ArrayList<UUID>();
                                }

                                thatWaitThis.add(otherChildTaskTaskId);
                            }

                            // @Wait stuff
//							if (isPromiseCollection()) {
//								for (Object obj : (Collection)arg.getObject())
//							}
                        }
                    }
                }


                // find external dependencies
                Set externalThatWaitThis = null;
                ArgContainer args[] = childTask.getArgs();

                if (args != null) {

                    Set internalDepsIds = null;

                    for (ArgContainer arg : args) {

                        boolean isPromise = arg.isPromise();

                        // skip not promises or resolved promises
                        if (!isPromise || (isPromise && arg.isReady())) {
                            continue;
                        }

                        // lazy initialization of Set
                        if (internalDepsIds == null) {

                            // create hash set of internal dependencies (list of task id)
                            internalDepsIds = new HashSet(childTasks.length);
                            for (TaskContainer otherChildTask : childTasks) {
                                internalDepsIds.add(otherChildTask.getTaskId());
                            }

                        }

                        // Is it external promise?
                        if (!internalDepsIds.contains(arg.getTaskId())) {
                            if (externalThatWaitThis == null) {
                                externalThatWaitThis = new HashSet();
                            }

                            externalThatWaitThis.add(arg.getTaskId());
                        }

                    }

                }


                boolean isReady = addDependency(childTask, taskId, thatWaitThis, externalThatWaitThis,
                        dependTaskId != null && dependTaskId.equals(childTaskId));

                if (isReady) {
                    logger.debug("analiseDecision() add new ready taskId() because addDependency() returns true. new taskId [{}]", childTaskId);

                    dependencyDecision.addReadyTask(childTaskId);
                }
            }
        }

        // recursion start to delete completed tasks and collect ready task list
        if (value == null ||
                (value != null && !value.isPromise()) ||
                (value != null && value.isPromise() && value.isReady())) {

            logger.debug("taskId: {}", taskId);
            TaskDependency taskDependency = id2depMap.get(taskId);

            removeFinishedTasks(taskDependency, dependencyDecision);

        }

        return dependencyDecision;
    }


    @Override
    public void startProcess(TaskContainer task) {
        logger.debug("startProcess taskId = [{}]", task.getTaskId());

        addDependency(task, null, null, null, false);
    }


    private void removeFinishedTasks(TaskDependency taskDependency, DependencyDecision dependencyDecision) {

        UUID taskId = taskDependency.getTaskId();

        logger.debug("removeFinishedTasks() taskId = [{}]", taskId);

        synchronized (taskDependency) {

            id2depMap.remove(taskId);

            // analise all waiting tasks
            // - decrement task.countdown for all tasks in taskMemory.waitingId list
            List<UUID> thatWaitThis = taskDependency.getThatWaitThis();
            if (thatWaitThis != null && !thatWaitThis.isEmpty()) {
                for (UUID thatTaskId : thatWaitThis) {

                    logger.debug("removeFinishedTasks() remove taskId [{}] from thisWaitThat list on [{}]", taskId, thatTaskId);

                    TaskDependency thatTaskDependency = id2depMap.get(thatTaskId);
                    List<UUID> thisWaitThat = thatTaskDependency.getThisWaitThat();

                    thisWaitThat.remove(taskId);

                    boolean isReady = thisWaitThat.isEmpty();

                    if (isReady) {
                        logger.debug("removeFinishedTasks() add new ready taskId = [{}]", thatTaskId);

                        dependencyDecision.addReadyTask(thatTaskId);
                    }
                }
            }
        }

        // analise parent and its "depend" state
        if (taskDependency.isParentWaitIt()) {

            logger.debug("task has parent task [{}]", taskDependency.getParentId());

            TaskDependency parentTaskDependency = id2depMap.get(taskDependency.getParentId());

            removeFinishedTasks(parentTaskDependency, dependencyDecision);
        } else {

            if (taskDependency.getParentId() == null) {

                dependencyDecision.setProcessFinished(true);
                dependencyDecision.setFinishedProcessId(taskDependency.getTaskId());
                // TODO: set real process value
                //dependencyDecision.setFinishedProcessValue(null);
            }

            // TODO: task may be not marked as isParentWaitIt but has parent task
            // So we can trigger finish of process only when all not daemon task will be finished.
        }

    }

    private boolean addDependency(TaskContainer task, UUID parentTaskId, List<UUID> thatWaitThis,
                                  Set<UUID> externalThatWaitThis,
                                  boolean isParentWaitIt) {

        logger.debug("addDependency() task = [{}], parentTaskId[{}], thatWaitThis = [{}], " +
                "externalThatWaitThis = [{}], isParentWaitIt = [{}]",
                task, parentTaskId, thatWaitThis, externalThatWaitThis, isParentWaitIt);

        UUID taskId = task.getTaskId();

        TaskDependency taskDependency = new TaskDependency();

        taskDependency.setTaskId(taskId);
        taskDependency.setParentId(parentTaskId);
        taskDependency.setThatWaitThis(thatWaitThis);
        taskDependency.setParentWaitIt(isParentWaitIt);

        List<UUID> thisWaitThat = null;

        ArgContainer[] argContainers = task.getArgs();

        if (argContainers != null) {

            TaskOptionsContainer taskOptionsContainer = task.getOptions();
            ArgType[] argTypes = null;

            if (taskOptionsContainer != null) {
                argTypes = taskOptionsContainer.getArgTypes();
            }

            logger.debug("addDependency() taskId [{}]. arg types = {}", taskId, argTypes);

            for (int i = 0; i < argContainers.length; i++) {

                // skip NoWait
                if (argTypes != null && ArgType.NO_WAIT.equals(argTypes[i])) {
                    continue;
                }

                ArgContainer argContainer = argContainers[i];

                if (argContainer != null && argContainer.isPromise() && !argContainer.isReady()) {
                    if (thisWaitThat == null) {
                        thisWaitThat = new LinkedList<UUID>();
                    }
                    thisWaitThat.add(argContainer.getTaskId());
                }
            }
        }

        logger.debug("addDependency() taskId [{}]. thisWaitThat.size() = {}", taskId,
                thisWaitThat == null ? 0 : thisWaitThat.size());

        boolean isReady = false;

        id2depMap.put(taskId, taskDependency);

        if (thisWaitThat != null) {

            taskDependency.setThisWaitThat(thisWaitThat);

            // TODO: register task id in external tasks and decrement countdown if not success
            if (externalThatWaitThis != null) {

                for (UUID externalWaitForTaskId : externalThatWaitThis) {
                    if (!registerExternalWaitFor(taskId, externalWaitForTaskId)) {
                        thisWaitThat.remove(externalWaitForTaskId);
                    }
                }

            }

            if (thisWaitThat.size() == 0) {
                isReady = true;
            }

        } else {
            isReady = true;
        }


        return isReady;
    }

    private boolean registerExternalWaitFor(UUID taskId, UUID externalWaitForTaskId) {

        logger.debug("registerExternalWaitFor() taskId = [{}], externalWaitForTaskId = [{}]", taskId, externalWaitForTaskId);

        TaskDependency taskDependency = id2depMap.get(externalWaitForTaskId);

        if (taskDependency == null) {
            return false;
        }

        // task state can be switched to done concurrently
        synchronized (taskDependency) {

            // this is double get because external finished task can be concurrently removed from map
            taskDependency = id2depMap.get(externalWaitForTaskId);

            if (taskDependency == null) {
                return false;
            }

            List<UUID> thatWaitThis = taskDependency.getThatWaitThis();

            if (thatWaitThis == null) {
                thatWaitThis = new LinkedList<UUID>();
                taskDependency.setThatWaitThis(thatWaitThis);
            }

            thatWaitThis.add(taskId);
        }

        return true;

    }

    public TaskDependency getTaskDependency(UUID taskId) {
        return id2depMap.get(taskId);
    }
}
