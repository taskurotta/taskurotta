package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.service.hz.TaskFatKey;
import ru.taskurotta.service.storage.TaskDao;
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

    private IMap<TaskFatKey, TaskContainer> id2TaskMap;
    private IMap<TaskFatKey, DecisionContainer> id2TaskDecisionMap;

    public HzTaskDao(HazelcastInstance hzInstance, String id2TaskMapName, String id2TaskDecisionMapName) {

        id2TaskMap = hzInstance.getMap(id2TaskMapName);
        id2TaskDecisionMap = hzInstance.getMap(id2TaskDecisionMapName);
    }

    @Override
    public void addDecision(DecisionContainer taskDecision) {
        logger.debug("Storing decision [{}]", taskDecision);
        id2TaskDecisionMap.set(new TaskFatKey(taskDecision.getProcessId(), taskDecision.getTaskId()), taskDecision);
    }

    @Override
    public TaskContainer getTask(UUID taskId, UUID processId) {
        return id2TaskMap.get(new TaskFatKey(processId, taskId));
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.set(new TaskFatKey(taskContainer.getProcessId(), taskContainer.getTaskId()), taskContainer, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        DecisionContainer result =  id2TaskDecisionMap.get(new TaskFatKey(processId, taskId));
        logger.debug("Getting decision [{}]", result);
        return result;
    }

    @Override
    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return id2TaskDecisionMap.containsKey(new TaskFatKey(processId, taskId));
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
        id2TaskMap.set(new TaskFatKey(taskContainer.getProcessId(), taskContainer.getTaskId()), taskContainer);
    }

    @Override
    public void deleteTasks(Set<UUID> taskIds, UUID processId) {
        for (UUID taskId : taskIds) {
            id2TaskMap.delete(new TaskFatKey(processId, taskId));
        }
    }

    @Override
    public void deleteDecisions(Set<UUID> decisionsIds, UUID processId) {
        for (UUID decisionId : decisionsIds) {
            id2TaskDecisionMap.delete(new TaskFatKey(processId, decisionId));
        }
    }

    @Override
    public void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds) {
        // do nothing
    }

    @Override
    public List<TaskContainer> findTasks(final TaskSearchCommand command) {
        List<TaskContainer> result = new ArrayList<>();
        if(command!=null && !command.isEmpty()) {
            result.addAll(Collections2.filter(id2TaskMap.values(), new Predicate<TaskContainer>() {

                private boolean hasText(String target){
                    return target != null && target.trim().length()>0;
                }

                private boolean isValid (TaskContainer taskContainer) {
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
