package ru.taskurotta.service.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: moroz
 * Date: 09.04.13
 */
public class MemoryTaskDao implements TaskDao {

    private final static Logger logger = LoggerFactory.getLogger(MemoryTaskDao.class);
    private static final UUID PASS = UUID.randomUUID();

    private Map<UUID, TaskContainer> id2TaskMap = new ConcurrentHashMap<>();
    private Map<UUID, Decision> id2TaskDecisionMap = new ConcurrentHashMap<>();

    private void lock(Object taskKey) {
    }

    private void unlock(Object taskKey) {
    }

    @Override
    public Decision finishTask(DecisionContainer taskDecision) {

        logger.debug("Storing decision [{}]", taskDecision);

        UUID taskKey = taskDecision.getTaskId();

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "finish task. Decision is " + decision);

            if (decision == null || decision.getState() == Decision.STATE_FINISH) {
                logger.warn("{}/{} Can not finish task. Task has {} state", taskDecision.getTaskId(), taskDecision.getProcessId(),
                        decision == null ? "null" : decision.getState());
                return null;
            }

            if (decision.getPass() != null && (taskDecision.getPass() == null || !(taskDecision.getPass().equals
                    (decision.getPass())))) {

                logger.warn("{}/{} Can not finish task. decision pass {} not equal to reference pass {}. Decision has" +
                                " been rejected", taskDecision.getTaskId(), taskDecision.getProcessId(), taskDecision.getPass(),
                        decision.getPass());
                return null;
            }

            // increment number of attempts for error tasks with retry policy
            if (taskDecision.containsError()) {
                decision.incrementErrorAttempts();
            }

            decision.setState(Decision.STATE_FINISH);
            decision.setDecisionContainer(taskDecision);
            decision.setRecoveryTime(0l);

            id2TaskDecisionMap.put(taskKey, decision);

            return decision;
        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public boolean retryTask(UUID taskId, UUID processId, long workerTimeout) {

        UUID taskKey = taskId;

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "retry task. Decision is " + decision);

            if (decision == null || decision.getState() != Decision.STATE_FINISH) {
                logger.warn("{}/{} Can not retry task. Task has {} state", taskId, processId,
                        decision == null ? "null" : decision.getState());

                return false;
            }

            long recoveryTime = System.currentTimeMillis() + workerTimeout;

            decision.setState(Decision.STATE_REGISTERED);
            decision.setDecisionContainer(null);
            decision.setRecoveryTime(recoveryTime);

            id2TaskDecisionMap.put(taskKey, decision);

            return true;

        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public void updateTimeout(UUID taskId, UUID processId, long workerTimeout) {

        UUID taskKey = taskId;

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

            if (decision == null) {
                // skip it
                return;
            }

            if (decision.getState() != Decision.STATE_REGISTERED) {
                logger.debug("{}/{} Can not start task. Task has {} state (not in registered state)",
                        taskId, processId, decision.getState());

                return;
            }

            long recoveryTime = System.currentTimeMillis() + workerTimeout;
            decision.setRecoveryTime(recoveryTime);

            id2TaskDecisionMap.put(taskKey, decision);

        } finally {
            unlock(taskKey);
        }

    }

    @Override
    public Decision getTaskDecision(UUID taskId, UUID processId) {

        UUID taskKey = taskId;

        return id2TaskDecisionMap.get(taskKey);
    }


    @Override
    public Decision startTask(UUID taskId, UUID processId, long workerTimeout, boolean failOnWorkerTimeout) {

        UUID taskKey = taskId;

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "start task. Decision is " + decision);


            if (decision != null && decision.getState() != Decision.STATE_REGISTERED) {
                logger.debug("{}/{} Can not start task. Task has {} state (not in registered state)",
                        taskId, processId, decision.getState());

                return null;
            }

//            UUID pass = UUID.randomUUID();
            long recoveryTime = System.currentTimeMillis() + workerTimeout;

            if (decision == null) {
                decision = new Decision(taskId, processId, Decision.STATE_WORK, null, recoveryTime, 0, null);
            } else {
                // assume that workerTimeout and failOnWorkerTimeouts values can not be changed

//                decision.setPass(pass);
                decision.setState(Decision.STATE_WORK);
                decision.setRecoveryTime(recoveryTime);
                decision.setDecisionContainer(null);
            }

            id2TaskDecisionMap.put(taskKey, decision);

            return decision;
        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public boolean restartTask(UUID taskId, UUID processId, boolean force, boolean ifFatalError) {

        UUID taskKey = taskId;

        lock(taskKey);

        try {
            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "restart task isForce = " + force + ". Decision is " + decision);

            if (decision == null) {
                return true;
            }

            if (!force) {

                if (ifFatalError) {
                    if (decision.getState() == Decision.STATE_FINISH) {
                        ErrorContainer errorContainer = decision.getDecisionContainer().getErrorContainer();

                        if (errorContainer == null || !errorContainer.isFatalError()) {

                            logger.debug("{}/{} Can not restart task. Task is finished now and has not fatal error. " +
                                    "Decision is {}", taskId, processId, decision
                                    .getState(), decision);
                            return false;
                        }
                    }
                } else if ((decision.getState() == Decision.STATE_FINISH)) {

                    logger.debug("{}/{} Can not restart task. Task is finished now. " +
                            "Decision is {}", taskId, processId, decision
                            .getState(), decision);
                    return false;
                }

            }

            // reset error counter for interrupted tasks
            if (ifFatalError) {
                decision.setErrorAttempts(0);
            }

            decision.setState(Decision.STATE_REGISTERED);
            decision.setDecisionContainer(null);
            decision.setRecoveryTime(0l);

            id2TaskDecisionMap.put(taskKey, decision);

            return true;
        } finally {
            unlock(taskKey);
        }

    }

    @Override
    public void updateTaskDecision(DecisionContainer taskDecision) {

        UUID taskKey = taskDecision.getTaskId();

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "update task decision. Decision is " + decision);

            if (decision == null) {
                logger.warn("{}/{} Can update task decision. Task decision nut found", taskKey, taskDecision
                        .getProcessId());
                return;
            }

            decision.setDecisionContainer(taskDecision);
            id2TaskDecisionMap.put(taskKey, decision);

        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public ResultSetCursor findIncompleteTasks(long lastRecoveryTime, int batchSize) {
        throw new UnsupportedOperationException("Please, use MongoTaskDao");
    }

    @Override
    public TaskContainer getTask(UUID taskId, UUID processId) {
        return id2TaskMap.get(taskId);
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
    }

    @Override
    public Decision getDecision(UUID taskId, UUID processId) {

        UUID taskKey = taskId;

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

            logger.debug("Getting decision [{}]", decision);

//            UN.put(taskKey.getTaskId(), "getDecision(). Decision is " + decision);

            return decision;

        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public DecisionContainer getDecisionContainer(UUID taskId, UUID processId) {
        Decision decision = getDecision(taskId, processId);

        if (decision != null) {
            return decision.getDecisionContainer();
        }

        return null;
    }


    @Override
    public boolean isTaskReleased(UUID taskId, UUID processId) {

        UUID taskKey = taskId;

        lock(taskKey);

        try {
            Decision decision = id2TaskDecisionMap.get(taskKey);
            if (decision == null) {
                return false;
            }

            return decision.getState() == Decision.STATE_FINISH;
        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
        Collection<TaskContainer> tasks = id2TaskMap.values();
        int pageEnd = pageSize * pageNumber >= tasks.size() ? tasks.size() : pageSize * pageNumber;
        int pageStart = (pageNumber - 1) * pageSize;
        List<TaskContainer> resultList = Arrays.asList(tasks.toArray(new TaskContainer[tasks.size()])).subList(pageStart, pageEnd);

        return new GenericPage<>(resultList, pageNumber, pageSize, id2TaskMap.size());
    }

    @Override
    public List<TaskContainer> getRepeatedTasks(final int iterationCount) {
        // @todo: implement method
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public void updateTask(TaskContainer taskContainer) {
        id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
    }

    @Override
    public void deleteTasks(Set<UUID> taskIds, UUID processId) {
        for (UUID taskId : taskIds) {
            id2TaskMap.remove(taskId);
        }
    }

    @Override
    public void deleteDecisions(Set<UUID> taskIds, UUID processId) {


        for (UUID taskId : taskIds) {

            UUID taskKey = taskId;

            lock(taskKey);

            try {
//                UN.put(taskKey.getTaskId(), "delete decision");

                id2TaskDecisionMap.remove(taskKey);
            } finally {
                unlock(taskKey);
            }
        }
    }

    @Override
    public void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds) {
        // do nothing
    }

    @Override
    public List<TaskContainer> findTasks(final TaskSearchCommand command) {
        List<TaskContainer> result = new ArrayList<>();
        if (command != null && !command.isEmpty()) {
            result.addAll(Collections2.filter(id2TaskMap.values(), new Predicate<TaskContainer>() {

                private boolean hasText(String target) {
                    return target != null && target.trim().length() > 0;
                }

                private boolean isValid(TaskContainer taskContainer) {
                    boolean isValid = true;
                    if (hasText(command.getTaskId())) {
                        isValid = isValid && taskContainer.getTaskId().toString().startsWith(command.getTaskId());
                    }
                    if (hasText(command.getProcessId())) {
                        isValid = isValid && taskContainer.getProcessId().toString().startsWith(command.getProcessId());
                    }
                    return isValid;
                }

                @Override
                public boolean apply(TaskContainer processVO) {
                    return isValid(processVO);
                }

            }));
        }
        return result;
    }
}
