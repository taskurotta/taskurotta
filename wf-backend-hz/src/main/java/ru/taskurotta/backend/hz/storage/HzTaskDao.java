package ru.taskurotta.backend.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TaskDao storing tasks in HZ shared maps
 * User: dimadin
 * Date: 11.06.13 18:13
 */
public class HzTaskDao implements TaskDao {

    private Map<TaskKey, TaskContainer> id2TaskMap;
    private Map<TaskKey, DecisionContainer> id2TaskDecisionMap;


    public HzTaskDao(HazelcastInstance hzInstance) {
        id2TaskMap = hzInstance.getMap("id2TaskMap");
        id2TaskDecisionMap = hzInstance.getMap("id2TaskDecisionMap");
    }

    @Override
    public void addDecision(DecisionContainer taskDecision) {
        id2TaskDecisionMap.put(new TaskKey(taskDecision.getProcessId(), taskDecision.getTaskId()), taskDecision);
    }

    @Override
    public TaskContainer getTask(UUID taskId, UUID processId) {
        return id2TaskMap.get(new TaskKey(processId, taskId));
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.put(new TaskKey(taskContainer.getProcessId(), taskContainer.getTaskId()), taskContainer);
    }

    @Override
    public DecisionContainer getDecision(UUID taskId) {
        return id2TaskDecisionMap.get(taskId);
    }

    @Override
    public boolean isTaskReleased(UUID taskId) {
        return id2TaskDecisionMap.containsKey(taskId);
    }

    @Override
    public List<TaskContainer> getProcessTasks(UUID processUuid) {
        if(processUuid == null) {
            return null;
        }
        List<TaskContainer> result = new ArrayList<>();
        for(TaskContainer tc: id2TaskMap.values()) {
            if(processUuid.equals(tc.getProcessId())) {
                result.add(tc);
            }
        }
        return result;
    }

    @Override
    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
        Collection<TaskContainer> tasks = id2TaskMap.values();
        int pageEnd = pageSize * pageNumber >= tasks.size() ? tasks.size() : pageSize * pageNumber;
        int pageStart = (pageNumber - 1) * pageSize;
        List<TaskContainer> resultList = Arrays.asList(tasks.toArray(new TaskContainer[tasks.size()])).subList(pageStart, pageEnd);

        return new GenericPage<>(resultList, pageNumber, pageSize, resultList.size());
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

    class TaskKey implements PartitionAware, Serializable {
        UUID processId;
        UUID taskId;

        public TaskKey(UUID processId, UUID taskId) {
            this.taskId = taskId;
            this.processId = processId;
        }

        @Override
        public Object getPartitionKey() {
            return processId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TaskKey)) return false;

            TaskKey taskKey = (TaskKey) o;

            if (!processId.equals(taskKey.processId)) return false;
            if (!taskId.equals(taskKey.taskId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = processId.hashCode();
            result = 31 * result + taskId.hashCode();
            return result;
        }
    }

}
