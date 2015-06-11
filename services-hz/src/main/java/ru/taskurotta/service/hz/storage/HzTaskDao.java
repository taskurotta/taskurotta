package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.service.hz.TaskKey;
import ru.taskurotta.service.storage.TaskDao;
import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.transport.model.DecisionContainer;
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

    public HzTaskDao(HazelcastInstance hzInstance, String id2TaskMapName, String id2TaskDecisionMapName) {

        id2TaskMap = hzInstance.getMap(id2TaskMapName);
        id2TaskDecisionMap = hzInstance.getMap(id2TaskDecisionMapName);
    }

    @Override
    public boolean finishTask(DecisionContainer taskDecision) {

        logger.debug("Storing decision [{}]", taskDecision);

        TaskKey taskKey = new TaskKey(taskDecision.getTaskId(), taskDecision.getProcessId());

        id2TaskDecisionMap.lock(taskKey);

        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);
            if (decision == null || decision.getState() != Decision.STATE_WORK) {
                logger.warn("{}/{} Can not finish task. Task has {} state", taskKey.getTaskId(), taskKey.getProcessId(),
                        decision == null ? "null" : decision.getState());
                return false;
            }

            if (decision.getPass() != null && (taskDecision.getPass() == null || !(taskDecision.getPass().equals
                    (decision.getPass())))) {

                logger.warn("{}/{} Can not finish task. decision pass {} not equal to reference pass {}. Decision has" +
                                " been rejected", taskKey.getTaskId(), taskKey.getProcessId(), taskDecision.getPass(),
                        decision.getPass());
                return false;
            }

            decision.setState(Decision.STATE_FINISH);
            decision.setDecisionContainer(taskDecision);
            decision.setRecoveryTime(0l);

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);
            return true;
        } finally {
            id2TaskDecisionMap.unlock(taskKey);
        }
    }

    // todo: save timeToStart
    @Override
    public boolean retryTask(UUID taskId, UUID processId, long timeToStart) {

        TaskKey taskKey = new TaskKey(taskId, processId);

        id2TaskDecisionMap.lock(taskKey);

        try {
            Decision decision = id2TaskDecisionMap.get(taskKey);
            if (decision == null || decision.getState() != Decision.STATE_FINISH) {
                logger.warn("{}/{} Can not retry task. Task has {} state", taskKey.getTaskId(), taskKey.getProcessId(),
                        decision == null ? "null" : decision.getState());

//                UN.print(taskKey.getTaskId());
//logger.error("{}/{} Current stack trace",  taskKey.getTaskId(), taskKey.getProcessId(), new Throwable("" + System.currentTimeMillis()));
                return false;
            }

            decision.setState(Decision.STATE_REGISTERED);
            decision.setDecisionContainer(null);
            decision.setRecoveryTime(0l);

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);
            return true;

        } finally {
            id2TaskDecisionMap.unlock(taskKey);
        }
    }

    @Override
    public UUID startTask(UUID taskId, UUID processId, long workerTimeout, boolean failOnWorkerTimeout) {

        TaskKey taskKey = new TaskKey(taskId, processId);

        id2TaskDecisionMap.lock(taskKey);
//        UN.put(taskKey.getTaskId());

        try {
            Decision decision = id2TaskDecisionMap.get(taskKey);
            if (decision != null && decision.getState() != Decision.STATE_REGISTERED) {
                logger.warn("{}/{} Can not start task. Task has {} state", taskKey.getTaskId(), taskKey.getProcessId(),
                        decision.getState());
                return null;
            }

            UUID pass = UUID.randomUUID();
            long recoveryTime = System.currentTimeMillis() + workerTimeout;

            if (decision == null) {
                decision = new Decision(taskId, processId, Decision.STATE_WORK, null, recoveryTime,
                        null);
            } else {
                // assume that workerTimeout and failOnWorkerTimeouts values can not be changed

//                decision.setPass(pass);
                decision.setState(Decision.STATE_WORK);
                decision.setRecoveryTime(recoveryTime);
                decision.setDecisionContainer(null);
            }

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

            return pass;
        } finally {
            id2TaskDecisionMap.unlock(taskKey);
        }
    }

    @Override
    public boolean restartTask(UUID taskId, UUID processId, long timeToStart, boolean force) {

        TaskKey taskKey = new TaskKey(taskId, processId);

        id2TaskDecisionMap.lock(taskKey);

        try {
            Decision decision = id2TaskDecisionMap.get(taskKey);
            if (decision == null) {
                return true;
            }

            if (!force && decision.getState() == Decision.STATE_FINISH) {
                logger.debug("{}/{} Can not restart task. Task is finished now. Decision is {}", taskKey.getTaskId(),
                        taskKey.getProcessId(), decision.getState(), decision);

                return false;
            }

            decision.setState(Decision.STATE_REGISTERED);
            decision.setDecisionContainer(null);
            decision.setRecoveryTime(0l);

            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);
            return true;

        } finally {
            id2TaskDecisionMap.unlock(taskKey);
        }

    }

    @Override
    public void updateTaskDecision(DecisionContainer taskDecision) {

        TaskKey taskKey = new TaskKey(taskDecision.getTaskId(), taskDecision.getProcessId());

        id2TaskDecisionMap.lock(taskKey);
        try {

            Decision decision = id2TaskDecisionMap.get(taskKey);
            if (decision == null) {
                logger.warn("{}/{} Can update task decision. Task decision nut found", taskKey.getTaskId(), taskKey
                        .getProcessId());
                return;
            }

            decision.setDecisionContainer(taskDecision);
            id2TaskDecisionMap.set(taskKey, decision, 0l, TimeUnit.NANOSECONDS);

        } finally {
            id2TaskDecisionMap.unlock(taskKey);
        }
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
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        Decision decision = id2TaskDecisionMap.get(new TaskKey(taskId, processId));
        if (decision == null) {
            return null;
        }

        DecisionContainer result = decision.getDecisionContainer();

        logger.debug("Getting decision [{}]", result);
        return result;
    }

    @Override
    public boolean isTaskReleased(UUID taskId, UUID processId) {
        Decision decision = id2TaskDecisionMap.get(new TaskKey(taskId, processId));
        if (decision == null) {
            return false;
        }

        return decision.getState() == Decision.STATE_FINISH;
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
        return new ArrayList<>(
                Collections2.filter(id2TaskMap.values(), new Predicate<TaskContainer>() {
                    @Override
                    public boolean apply(TaskContainer taskContainer) {
                        return taskContainer.getErrorAttempts() >= iterationCount;
                    }
                }));
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
    public void deleteDecisions(Set<UUID> decisionsIds, UUID processId) {
        for (UUID decisionId : decisionsIds) {
            id2TaskDecisionMap.delete(new TaskKey(decisionId, processId));
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
