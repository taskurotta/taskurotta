package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.InterruptedTaskExt;
import ru.taskurotta.service.console.model.InterruptedTaskExt.InterruptedTaskType;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;
import ru.taskurotta.service.hz.support.PredicateUtils;
import ru.taskurotta.service.storage.AbstractInterruptedTasksService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.InterruptedTaskSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ru.taskurotta.service.console.model.InterruptedTaskExt.InterruptedTaskType.KNOWN;
import static ru.taskurotta.service.console.model.InterruptedTaskExt.InterruptedTaskType.UNKNOWN;

/**
 * Created on 19.03.2015.
 */
public class HzInterruptedTasksService extends AbstractInterruptedTasksService implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(HzInterruptedTasksService.class);

    private IMap<UUID, InterruptedTaskExt> storeIMap;

    public HzInterruptedTasksService(HazelcastInstance hazelcastInstance, String storeMapName, String scriptLocation, long scriptReloadTimeout) {
        super(scriptLocation, scriptReloadTimeout);
        this.storeIMap = hazelcastInstance.getMap(storeMapName);
    }

    @Override
    public void save(InterruptedTask task, String message, String stackTrace) {
        InterruptedTaskType taskType = isKnown(task) ? KNOWN : UNKNOWN;
        storeIMap.put(task.getTaskId(), new InterruptedTaskExt(task, message, stackTrace, taskType));
    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand searchCommand) {

        logger.debug("Try to find interrupted tasks by searchCommand [{}]", searchCommand);

        List<Predicate> predicates = new ArrayList<>();

        if (searchCommand.getTaskId() != null) {
            Collection<InterruptedTask> result = null;
            InterruptedTask bp = storeIMap.get(searchCommand.getTaskId());
            if (bp != null) {
                result = new ArrayList<>();
                result.add(bp);
            }
            return result;
        }


        if (StringUtils.hasText(searchCommand.getActorId())) {
            predicates.add(PredicateUtils.getStartsWith("actorId", searchCommand.getActorId()));
        }

        if (StringUtils.hasText(searchCommand.getStarterId())) {
            predicates.add(PredicateUtils.getStartsWith("starterId", searchCommand.getStarterId()));
        }

        if (searchCommand.getProcessId() != null) {
            predicates.add(PredicateUtils.getEqual("processId", searchCommand.getProcessId()));
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            predicates.add(PredicateUtils.getStartsWith("errorClassName", searchCommand.getErrorClassName()));
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            predicates.add(PredicateUtils.getStartsWith("errorMessage", searchCommand.getErrorMessage()));
        }

        if (searchCommand.getEndPeriod() > 0) {
            predicates.add(PredicateUtils.getLessThen("time", searchCommand.getEndPeriod()));
        }

        if (searchCommand.getStartPeriod() > 0) {
            predicates.add(PredicateUtils.getMoreThen("time", searchCommand.getStartPeriod()));
        }

        Collection<InterruptedTask> result;
        if (predicates.isEmpty()) {
            result = findAll();
        } else {
            Predicate predicate = PredicateUtils.combineWithAndCondition(predicates);
            result = asSimpleTasks(storeIMap.values(predicate));
        }
        logger.trace("Found [{}] interrupted tasks by command[{}]", result != null ? result.size() : null, searchCommand);
        return result;
    }

    //TODO: try to remove this method invocations
    public Collection<InterruptedTask> asSimpleTasks(Collection<InterruptedTaskExt> tasks) {
        Collection<InterruptedTask> result = null;
        if (tasks != null && !tasks.isEmpty()) {
            result = new ArrayList<>();
            for (InterruptedTaskExt tExt : tasks) {
                tExt.setFullMessage(null);
                tExt.setStackTrace(null);
                result.add(tExt);
            }
        }

        return result;
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        return asSimpleTasks(storeIMap.values());
    }

    @Override
    public void delete(UUID processId, UUID taskId) {
        storeIMap.delete(taskId);
    }

    @Override
    public String getFullMessage(UUID processId, UUID taskId) {
        InterruptedTaskExt bp = storeIMap.get(taskId);
        return bp != null ? bp.getFullMessage() : null;
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        InterruptedTaskExt bp = storeIMap.get(taskId);
        return bp != null ? bp.getStackTrace() : null;
    }

    @Override
    public List<TasksGroupVO> getGroupList(GroupCommand command) {
        List<TasksGroupVO> result = null;
        Collection<InterruptedTask> tasks = find(command);

        if (tasks != null && !tasks.isEmpty()) {
            Map<String, Collection<InterruptedTask>> groupedTasks = InterruptedTaskSupport.groupProcessList(tasks, command.getGroup());
            result = InterruptedTaskSupport.convertToGroupsList(groupedTasks, command);
        }

        return result;
    }

    @Override
    public Collection<TaskIdentifier> getTaskIdentifiers(GroupCommand command) {
        return InterruptedTaskSupport.asTaskIdentifiers(find(command), command);
    }

    @Override
    public Set<UUID> getProcessIds(GroupCommand command) {
        return InterruptedTaskSupport.asProcessIdentifiers(find(command), command);
    }

    @Override
    public long deleteTasksForProcess(UUID processId) {
        long result = 0l;
        Collection<InterruptedTaskExt> tasks = storeIMap.values(new Predicates.EqualPredicate("processId", processId));
        if (tasks != null && !tasks.isEmpty()) {//TODO: lock map for the operation
            for (InterruptedTaskExt task : tasks) {
                if (storeIMap.containsKey(task.getTaskId())) {
                    storeIMap.delete(task.getTaskId());
                    result++;
                }
            }
        }
        return result;
    }

    @Override
    public int getKnowInterruptedTasksCount() {
        return storeIMap.values(new Predicates.EqualPredicate("type", InterruptedTaskType.KNOWN)).size();
    }
}
