package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TaskDao storing tasks in HZ shared(and processId partitioned) maps
 * Date: 11.06.13 18:13
 */
public class HzTaskDao implements TaskDao {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskDao.class);

    private IMap<TaskKey, TaskContainer> id2TaskMap;
    private IMap<TaskKey, Decision> id2TaskDecisionMap;

//    private ConcurrentHashMap<TaskKey, String> checkpoint = new ConcurrentHashMap<>(10000, 0.8f, 100);

    private void lock(TaskKey taskKey) {

        id2TaskDecisionMap.lock(taskKey);

//        if (checkpoint.putIfAbsent(taskKey, "") != null) {
//            logger.error("\n\n\n\n\n\n\n==========================\n===================\nUMBAAAAAAAAAAAAAAAAAAAAAA!");
//        }
    }

    private void unlock(TaskKey taskKey) {

//        if (checkpoint.remove(taskKey) == null) {
//            logger.error("\n\n\n\n\n\n\n==========================\n" +
//                    "===================\nUMBAAAAAAAAAAAAAAAAAAAAAA2222" +
//                    "!");
//        }

        id2TaskDecisionMap.unlock(taskKey);

    }

    public HzTaskDao(HazelcastInstance hzInstance, String id2TaskMapName, String id2TaskDecisionMapName) {

        id2TaskMap = hzInstance.getMap(id2TaskMapName);
        id2TaskDecisionMap = hzInstance.getMap(id2TaskDecisionMapName);
    }

    @Override
    public Decision finishTask(DecisionContainer taskDecision) {

        logger.debug("Storing decision [{}]", taskDecision);

        TaskKey taskKey = new TaskKey(taskDecision.getTaskId(), taskDecision.getProcessId());

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "finish task. Decision is " + decision);

            if (decision == null || decision.getState() == Decision.STATE_FINISH) {
                logger.warn("{}/{} Can not finish task. Task has {} state", taskKey.getTaskId(), taskKey.getProcessId(),
                        decision == null ? "null" : decision.getState());
                return null;
            }

            if (decision.getPass() != null && (taskDecision.getPass() == null || !(taskDecision.getPass().equals
                    (decision.getPass())))) {

                logger.warn("{}/{} Can not finish task. decision pass {} not equal to reference pass {}. Decision has" +
                                " been rejected", taskKey.getTaskId(), taskKey.getProcessId(), taskDecision.getPass(),
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

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

            return decision;
        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public boolean retryTask(UUID taskId, UUID processId) {

        TaskKey taskKey = new TaskKey(taskId, processId);

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "retry task. Decision is " + decision);

            if (decision == null || decision.getState() != Decision.STATE_FINISH) {
                logger.warn("{}/{} Can not retry task. Task has {} state", taskId, processId,
                        decision == null ? "null" : decision.getState());

                return false;
            }

            decision.setState(Decision.STATE_REGISTERED);
            decision.setDecisionContainer(null);
            decision.setRecoveryTime(0l);

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

            return true;

        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public void updateTimeout(UUID taskId, UUID processId, long workerTimeout) {

        TaskKey taskKey = new TaskKey(taskId, processId);

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

            if (decision == null) {
                // skip it
                return;
            }

            if (decision.getState() != Decision.STATE_WORK) {
                logger.debug("{}/{} Can not update timeout. Task has {} state (not in work state)",
                        taskId, processId, decision.getState());

                return;
            }

            long recoveryTime = System.currentTimeMillis() + workerTimeout;
            decision.setRecoveryTime(recoveryTime);

            id2TaskDecisionMap.set(taskKey, decision, 0L, TimeUnit.NANOSECONDS);

        } finally {
            unlock(taskKey);
        }

    }

    @Override
    public Decision startTask(UUID taskId, UUID processId, long workerTimeout, boolean failOnWorkerTimeout) {

        TaskKey taskKey = new TaskKey(taskId, processId);

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

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

            return decision;
        } finally {
            unlock(taskKey);
        }
    }

    @Override
    public boolean restartTask(UUID taskId, UUID processId, boolean force, boolean ifFatalError) {

        TaskKey taskKey = new TaskKey(taskId, processId);

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

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

            return true;
        } finally {
            unlock(taskKey);
        }

    }

    @Override
    public void updateTaskDecision(DecisionContainer taskDecision) {

        TaskKey taskKey = new TaskKey(taskDecision.getTaskId(), taskDecision.getProcessId());

        lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);

//            UN.put(taskKey.getTaskId(), "update task decision. Decision is " + decision);

            if (decision == null) {
                logger.warn("{}/{} Can update task decision. Task decision nut found", taskKey.getTaskId(), taskKey
                        .getProcessId());
                return;
            }

            decision.setDecisionContainer(taskDecision);
            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

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
        return id2TaskMap.get(new TaskKey(taskId, processId));
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.set(new TaskKey(taskContainer.getTaskId(), taskContainer.getProcessId()), taskContainer, 0,
                TimeUnit.NANOSECONDS);
    }

    @Override
    public Decision getDecision(UUID taskId, UUID processId) {

        TaskKey taskKey = new TaskKey(taskId, processId);

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

        TaskKey taskKey = new TaskKey(taskId, processId);

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
        id2TaskMap.set(new TaskKey(taskContainer.getTaskId(), taskContainer.getProcessId()), taskContainer);
    }

    @Override
    public void deleteTasks(Set<UUID> taskIds, UUID processId) {
        for (UUID taskId : taskIds) {
            id2TaskMap.delete(new TaskKey(taskId, processId));
        }
    }

    @Override
    public void deleteDecisions(Set<UUID> taskIds, UUID processId) {


        for (UUID taskId : taskIds) {

            TaskKey taskKey = new TaskKey(taskId, processId);

            lock(taskKey);

            try {
//                UN.put(taskKey.getTaskId(), "delete decision");

                id2TaskDecisionMap.delete(taskKey);
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
