package ru.taskurotta.service.recovery.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.recovery.TaskRecoveryService;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Created on 24.04.2015.
 */
public class TaskRecoveryServiceImpl implements TaskRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(TaskRecoveryServiceImpl.class);

    private DependencyService dependencyService;
    private TaskService taskService;
    private QueueService queueService;
    private ProcessService processService;
    private InterruptedTasksService interruptedTasksService;

    public TaskRecoveryServiceImpl(DependencyService dependencyService, TaskService taskService, QueueService queueService, InterruptedTasksService interruptedTasksService, ProcessService processService) {
        this.dependencyService = dependencyService;
        this.taskService = taskService;
        this.queueService = queueService;
        this.interruptedTasksService = interruptedTasksService;
        this.processService = processService;
    }

    @Override
    public boolean recover(final UUID processId, final UUID taskId) {//acquires lock on graph
        final boolean[] boolContainer = new boolean[1];
        dependencyService.changeGraph(new GraphDao.Updater() {

            @Override
            public UUID getProcessId() {
                return processId;
            }

            @Override
            public boolean apply(Graph graph) {
                boolean result = false;
                long touchTime = System.currentTimeMillis();
                graph.setTouchTimeMillis(touchTime);

                if (taskService.restartTask(taskId, processId, System.currentTimeMillis(), true)) {
                    TaskContainer tc = taskService.getTask(taskId, processId);
                    if (tc != null && queueService.enqueueItem(tc.getActorId(), tc.getTaskId(), tc.getProcessId(), System.currentTimeMillis(), TransportUtils.getTaskList(tc))) {
                        interruptedTasksService.delete(processId, taskId);
                        result = true;
                    }
                }

                // --process state change part--
                if (hasOtherNotReadyFatalTasks(processId, taskId, graph.getAllReadyItems())) {
                    processService.markProcessAsBroken(processId);
                } else {
                    processService.markProcessAsStarted(processId);
                }
                // --/process state change part--

                boolContainer[0] = result;

                logger.debug("ProcessId [{}] graph change applied: touch time updated [{}], result[{}]", processId, touchTime, result);
                return result;
            }

        });

        return boolContainer[0];
    }

    private boolean hasOtherNotReadyFatalTasks(UUID processId, UUID taskId, Map<UUID, Long> readyItems) {
        boolean result = false;
        if (readyItems!=null) {
            for (UUID readyTaskId : readyItems.keySet()) {
                if (!taskId.equals(readyTaskId) && TransportUtils.hasFatalError(taskService.getDecision(readyTaskId, processId))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }


}
