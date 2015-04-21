package ru.taskurotta.service.hz.storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public class HzInterruptedTasksService implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(HzInterruptedTasksService.class);

    private static final String WILDCARD_SYMBOL = "%";

    private IMap<UUID, InterruptedTask> storeIMap;

    private IExecutorService executorService;

    private TaskService taskService;

    private QueueService queueService;

    private static HzInterruptedTasksService instance;

    public HzInterruptedTasksService(HazelcastInstance hazelcastInstance, String storeMapName, TaskService taskService, QueueService queueService) {
        this.storeIMap = hazelcastInstance.getMap(storeMapName);
        this.executorService = hazelcastInstance.getExecutorService(getClass().getName());
        this.taskService = taskService;
        this.queueService = queueService;
        this.instance = this;
    }

    @Override
    public void save(InterruptedTask task) {
        storeIMap.put(task.getTaskId(), task);
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
            result = storeIMap.values();
        } else {
            Predicate[] predicateArray = new Predicate[predicates.size()];
            Predicate predicate = new Predicates.AndPredicate(predicates.toArray(predicateArray));
            result = storeIMap.values(predicate);
        }
        logger.trace("Found [{}] interrupted tasks by command[{}]", result!=null?result.size():null, searchCommand);
        return result;
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        return storeIMap.values();
    }

    @Override
    public void delete(UUID processId, UUID taskId) {
        storeIMap.delete(taskId);
    }

    @Override
    public void restart(final UUID processId, final UUID taskId) {
        executorService.execute(new RestartInterruptedTask(processId, taskId));
    }

    public static class RestartInterruptedTask implements Runnable, Serializable {

        private static final Logger logger = LoggerFactory.getLogger(RestartInterruptedTask.class);

        private UUID processId;
        private UUID taskId;

        public RestartInterruptedTask(UUID processId, UUID taskId) {
            this.processId = processId;
            this.taskId = taskId;
        }

        @Override
        public void run() {
            HzInterruptedTasksService nodeInstance = HzInterruptedTasksService.getInstance();
            TaskService nodeTaskService = nodeInstance.taskService;
            QueueService nodeQueueService = nodeInstance.queueService;
            if (nodeTaskService.restartTask(taskId, processId, System.currentTimeMillis(), true)) {
                TaskContainer tc = nodeTaskService.getTask(taskId, processId);
                if (tc != null && nodeQueueService.enqueueItem(tc.getActorId(), tc.getTaskId(), tc.getProcessId(), System.currentTimeMillis(), TransportUtils.getTaskList(tc))) {
                    nodeInstance.delete(processId, taskId);
                    logger.debug("taskId[{}], processId[{}] restarted and removed from store", taskId, processId);
                }
            }
        }
    }

    public static HzInterruptedTasksService getInstance() {
        return instance;
    }
}
