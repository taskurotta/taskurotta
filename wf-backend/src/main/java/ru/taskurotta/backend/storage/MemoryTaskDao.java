package ru.taskurotta.backend.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: moroz
 * Date: 09.04.13
 */
public class MemoryTaskDao implements TaskDao {

    private final static Logger logger = LoggerFactory.getLogger(MemoryTaskDao.class);

    private Map<UUID, TaskContainer> id2TaskMap = new ConcurrentHashMap<>();
    private Map<UUID, DecisionContainer> id2TaskDecisionMap = new ConcurrentHashMap<>();


    @Override
    public void addDecision(DecisionContainer taskDecision) {
        id2TaskDecisionMap.put(taskDecision.getTaskId(), taskDecision);
    }

    @Override
    public TaskContainer getTask(UUID taskId) {
        return id2TaskMap.get(taskId);
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
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
        if (processUuid == null) {
            return null;
        }
        List<TaskContainer> result = new ArrayList<>();
        for (TaskContainer tc : id2TaskMap.values()) {
            if (processUuid.equals(tc.getProcessId())) {
                result.add(tc);
            }
        }
        return result;
    }

    @Override
    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize) {
        logger.trace("listTasks called");
        List<TaskContainer> tmpResult = new ArrayList<>();
        int startIndex = (pageNumber - 1) * pageSize + 1;
        int endIndex = startIndex + pageSize - 1;
        long totalCount = 0;
        int index = 0;
        for (TaskContainer tc : id2TaskMap.values()) {
            if (index > endIndex) {
                totalCount = id2TaskMap.values().size();
                break;
            } else if (index >= startIndex && index <= endIndex) {
                tmpResult.add(tc);
            }
            index++;
        }

        return new GenericPage<>(tmpResult, pageNumber, pageSize, totalCount);
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
        //No need to implement it for in-memory storage case
    }

    @Override
    public TaskContainer removeTask(UUID taskId) {
        return id2TaskMap.remove(taskId);
    }
}
