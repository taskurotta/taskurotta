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
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.InterruptedTaskSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public class HzInterruptedTasksService implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(HzInterruptedTasksService.class);

    private static final String WILDCARD_SYMBOL = "%";

    private IMap<UUID, InterruptedTaskExt> storeIMap;

    public HzInterruptedTasksService(HazelcastInstance hazelcastInstance, String storeMapName) {
        this.storeIMap = hazelcastInstance.getMap(storeMapName);
    }

    @Override
    public void save(InterruptedTask task, String message, String stackTrace) {
        storeIMap.put(task.getTaskId(), new InterruptedTaskExt(task, message, stackTrace));
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
            predicates.add(new Predicates.LikePredicate("actorId", searchCommand.getActorId() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getStarterId())) {
            predicates.add(new Predicates.LikePredicate("starterId", searchCommand.getStarterId() + WILDCARD_SYMBOL));
        }

        if (searchCommand.getProcessId() != null) {
            predicates.add(new Predicates.EqualPredicate("processId", searchCommand.getProcessId()));
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            predicates.add(new Predicates.LikePredicate("errorClassName", searchCommand.getErrorClassName() + WILDCARD_SYMBOL));
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            predicates.add(new Predicates.LikePredicate("errorMessage", searchCommand.getErrorMessage() + WILDCARD_SYMBOL));
        }

        if (searchCommand.getEndPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", 0l, searchCommand.getEndPeriod()));
        }

        if (searchCommand.getStartPeriod() > 0) {
            predicates.add(new Predicates.BetweenPredicate("time", searchCommand.getStartPeriod(), Long.MAX_VALUE));
        }

        Collection<InterruptedTask> result = null;
        if (predicates.isEmpty()) {
            result = findAll();
        } else {
            Predicate[] predicateArray = new Predicate[predicates.size()];
            Predicate predicate = new Predicates.AndPredicate(predicates.toArray(predicateArray));
            result = asSimpleTasks(storeIMap.values(predicate));
        }
        logger.trace("Found [{}] interrupted tasks by command[{}]", result!=null?result.size():null, searchCommand);
        return result;
    }

    //TODO: try to remove this method invocations
    public Collection<InterruptedTask> asSimpleTasks(Collection<InterruptedTaskExt> tasks) {
        Collection<InterruptedTask> result = null;
        if (tasks!=null && !tasks.isEmpty()) {
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
        return bp!=null? bp.getFullMessage() : null;
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        InterruptedTaskExt bp = storeIMap.get(taskId);
        return bp!=null? bp.getStackTrace() : null;
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
        if (tasks!=null && !tasks.isEmpty()) {//TODO: lock map for the operation
            for (InterruptedTaskExt task : tasks) {
                if (storeIMap.containsKey(task.getTaskId())) {
                    storeIMap.delete(task.getTaskId());
                    result++;
                }
            }
        }
        return result;
    }

}
