package ru.taskurotta.service.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: remove dirty synchronization!
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
    public TaskContainer getTask(UUID taskId, UUID processId) {
        return id2TaskMap.get(taskId);
    }

    @Override
    public void addTask(TaskContainer taskContainer) {
        id2TaskMap.put(taskContainer.getTaskId(), taskContainer);
    }

    @Override
    public DecisionContainer getDecision(UUID taskId, UUID processId) {
        return id2TaskDecisionMap.get(taskId);
    }

    /**
     * @param taskId
     * @param processId
     * @return
     * @todo Graph should be used for this purpose.
     */
    @Override
    public boolean isTaskReleased(UUID taskId, UUID processId) {
        return id2TaskDecisionMap.containsKey(taskId);
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
    public void deleteTasks(Set<UUID> taskIds, UUID processId) {
        for (UUID taskId : taskIds) {
            id2TaskMap.remove(taskId);
        }
    }

    @Override
    public void deleteDecisions(Set<UUID> decisionsIds, UUID processId) {
        for (UUID decisionId : decisionsIds) {
            id2TaskDecisionMap.remove(decisionId);
        }
    }

    @Override
    public void archiveProcessData(UUID processId, Collection<UUID> finishedTaskIds) {
        for (UUID finishedTaskId : finishedTaskIds) {
            id2TaskMap.remove(finishedTaskId);
        }
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
