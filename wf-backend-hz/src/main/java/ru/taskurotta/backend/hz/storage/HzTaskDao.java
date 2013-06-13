package ru.taskurotta.backend.hz.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hazelcast.core.HazelcastInstance;
import net.sf.cglib.core.CollectionUtils;
import net.sf.cglib.core.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.storage.TaskDao;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * TaskDao storing tasks in HZ shared maps
 * User: dimadin
 * Date: 11.06.13 18:13
 */
public class HzTaskDao implements TaskDao {

    private final static Logger logger = LoggerFactory.getLogger(HzTaskDao.class);

    private Map<UUID, TaskContainer> id2TaskMap;
    private Map<UUID, DecisionContainer> id2TaskDecisionMap;


    public HzTaskDao(HazelcastInstance hzInstance) {
        id2TaskMap = hzInstance.getMap("id2TaskMap");
        id2TaskDecisionMap = hzInstance.getMap("id2TaskDecisionMap");
    }

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

        return new GenericPage(tmpResult, pageNumber, pageSize, totalCount);
    }

    @Override
    public List<TaskContainer> getRepeatedTasks(final int iterationCount) {
        List<TaskContainer> result = new ArrayList(CollectionUtils.filter(id2TaskMap.values(), new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                TaskContainer task = (TaskContainer) o;
                return task.getNumberOfAttempts() >= iterationCount;
            }
        }));
        return result;
    }

    @Override
    public void updateTask(TaskContainer taskContainer) {

    }

}
