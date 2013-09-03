package ru.taskurotta.backend.hz.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.hz.TaskKey;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * TaskDao storing tasks in HZ shared(and processId partitioned) maps
 * User: dimadin
 * Date: 11.06.13 18:13
 */
public class HzTaskDao implements TaskDao {

    private HazelcastInstance hzInstance;

    private String id2TaskMapName = "id2TaskMap";
    private String id2TaskDecisionMapName = "id2TaskDecisionMap";
    private IMap<TaskKey, TaskContainer> id2TaskMap;
    private IMap<TaskKey, DecisionContainer> id2TaskDecisionMap;

    public HzTaskDao(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    public void init() {
        id2TaskMap = hzInstance.getMap(id2TaskMapName);
        id2TaskDecisionMap = hzInstance.getMap(id2TaskDecisionMapName);
    }

    @Override
    public void addDecision(DecisionContainer taskDecision) {
        id2TaskDecisionMap.put(new TaskKey(taskDecision.getProcessId(), taskDecision.getTaskId()), taskDecision, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public TaskContainer getTask(UUID taskId, UUID processId) {
        return id2TaskMap.get(new TaskKey(processId, taskId));
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.set(new TaskKey(taskContainer.getProcessId(), taskContainer.getTaskId()), taskContainer, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        return id2TaskDecisionMap.get(new TaskKey(processId, taskId));
    }

    @Override
    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return id2TaskDecisionMap.containsKey(new TaskKey(processId, taskId));
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
        return (List<TaskContainer>) Collections2.filter(id2TaskMap.values(), new Predicate<TaskContainer>() {
            @Override
            public boolean apply(TaskContainer taskContainer) {
                return taskContainer.getNumberOfAttempts() >= iterationCount;
            }
        });
    }

    @Override
    public void updateTask(TaskContainer taskContainer) {

    }

    @Override
    public TaskContainer removeTask(UUID taskId, UUID processId) {
        return id2TaskMap.remove(new TaskKey(processId, taskId));
    }

    @Override
    public void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds) {
        // do nothing
    }

    public void setId2TaskMapName(String id2TaskMapName) {
        this.id2TaskMapName = id2TaskMapName;
    }

    public void setId2TaskDecisionMapName(String id2TaskDecisionMapName) {
        this.id2TaskDecisionMapName = id2TaskDecisionMapName;
    }

}
