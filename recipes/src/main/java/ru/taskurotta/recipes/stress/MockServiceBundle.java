package ru.taskurotta.recipes.stress;

import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.recipes.multiplier.MultiplierDecider;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.config.model.ActorPreferences;
import ru.taskurotta.service.config.model.ExpirationPolicyConfig;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.dependency.model.DependencyDecision;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.utils.TransportUtils;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 11:10 AM
 */
public class MockServiceBundle implements ServiceBundle {

    private static final UUID PASS = UUID.randomUUID();

    private static TaskContainer createRandomMultiplyTaskContainer() {
        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        return createRandomMultiplyTaskContainer(taskId, processId);
    }

    private static TaskContainer createRandomMultiplyTaskContainer(UUID taskId, UUID processId) {
        String actorId = ActorUtils.getActorId(ActorDefinition.valueOf(MultiplierDecider.class));

        String a = Integer.toString(ThreadLocalRandom.current().nextInt());
        String b = Integer.toString(ThreadLocalRandom.current().nextInt());

        return new TaskContainer(taskId, processId, PASS, "multiply",
                actorId, TaskType.DECIDER_START, 0, 1, new ArgContainer[]{
                new ArgContainer("java.lang.Integer", ArgContainer.ValueType.PLAIN, null, true, false, a),
                new ArgContainer("java.lang.Integer", ArgContainer.ValueType.PLAIN, null, true, false, b)},
                new TaskOptionsContainer(new ArgType[]{ArgType.NONE, ArgType.NONE}), false, null);
    }

    @Override
    public ProcessService getProcessService() {
        return new ProcessService() {
            @Override
            public void startProcess(TaskContainer task) {
                // ignore
            }

            @Override
            public void finishProcess(UUID processId, String returnValue) {
                // ignore
            }

            @Override
            public void deleteProcess(UUID processId) {

            }

            @Override
            public TaskContainer getStartTask(UUID processId) {
                return createRandomMultiplyTaskContainer();
            }

            @Override
            public void markProcessAsBroken(UUID processId) {

            }

            @Override
            public void markProcessAsStarted(UUID processId) {

            }

            @Override
            public ru.taskurotta.service.console.model.Process getProcess(UUID processUUID) {
                return null;
            }

            @Override
            public ResultSetCursor<UUID> findProcesses(long recoveryTime, int limit) {
                return new ResultSetCursor<UUID>() {
                    @Override
                    public Collection<UUID> getNext() {
                        return Collections.EMPTY_LIST;
                    }

                    @Override
                    public void close() throws IOException {

                    }
                };
            }
        };
    }

    @Override
    public TaskService getTaskService() {
        return new TaskService() {
            @Override
            public void startProcess(TaskContainer taskContainer) {
                // ignore
            }

            @Override
            public TaskContainer getTaskToExecute(UUID taskId, UUID processId, boolean simulate) {
                return createRandomMultiplyTaskContainer(taskId, processId);
            }

            @Override
            public TaskContainer getTask(UUID taskId, UUID processId) {
                return createRandomMultiplyTaskContainer(taskId, processId);
            }

            @Override
            public boolean finishTask(DecisionContainer taskDecision) {
                // ignore
                return true;
            }

            @Override
            public boolean retryTask(UUID taskId, UUID processId, long timeToStart) {
                return true;
            }

            @Override
            public boolean restartTask(UUID taskId, UUID processId, long timeToStart, boolean force) {
                return true;
            }

            @Override
            public DecisionContainer getDecision(UUID taskId, UUID processId) {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public List<TaskContainer> getAllRunProcesses() {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public List<DecisionContainer> getAllTaskDecisions(UUID processId) {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public void finishProcess(UUID processId, Collection<UUID> finishedTaskIds) {
                // ignore
            }

            @Override
            public void updateTaskDecision(DecisionContainer taskDecision) {

            }
        };
    }

    @Override
    public QueueService getQueueService() {
        return new QueueService() {

            // not atomic
            private int counter = 0;
            private int maxMemoMegabytes = 0;

            @Override
            public TaskQueueItem poll(String actorId, String taskList) {

                TaskQueueItem taskQueueItem = new TaskQueueItem();
                taskQueueItem.setEnqueueTime(0);
                taskQueueItem.setProcessId(UUID.randomUUID());
                taskQueueItem.setStartTime(0);
                taskQueueItem.setTaskId(UUID.randomUUID());
                taskQueueItem.setTaskList(taskList);

                counter++;
                int currentMemMegaBytes = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);
                if (maxMemoMegabytes < currentMemMegaBytes) {
                    maxMemoMegabytes = currentMemMegaBytes;
                }

                if (counter % 5000 == 0) {
                    System.err.println("Mock queue service poll: " + counter + " mem: " + maxMemoMegabytes);
                    maxMemoMegabytes = 0;
                }


                return taskQueueItem;
            }

            @Override
            public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {
                return true;
            }

            @Override
            public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
                return false;
            }

            @Override
            public String createQueueName(String actorId, String taskList) {
                return TransportUtils.createQueueName(actorId, taskList);
            }

            @Override
            public long getLastPolledTaskEnqueueTime(String queueName) {
                return System.currentTimeMillis();
            }

            @Override
            public List<String> getQueueNames() {
                return null;
            }

            @Override
            public Collection<String> getQueueNames(String prefix, String filter, boolean prefixStrip) {
                return null;
            }
        };
    }

    @Override
    public DependencyService getDependencyService() {
        return new DependencyService() {
            @Override
            public DependencyDecision applyDecision(DecisionContainer taskDecision) {

                DependencyDecision dependencyDecision = new DependencyDecision(taskDecision.getProcessId());
                dependencyDecision.setProcessFinished(true);
                dependencyDecision.setFinishedProcessValue("Done!");

                return dependencyDecision;
            }

            @Override
            public void startProcess(TaskContainer task) {
                // ignore
            }

            @Override
            public Graph getGraph(UUID processId) {
                return new Graph(processId, processId);
            }

            @Override
            public boolean changeGraph(GraphDao.Updater updater) {
                return true;
            }
        };
    }

    @Override
    public ConfigService getConfigService() {
        return new ConfigService() {
            @Override
            public boolean isActorBlocked(String actorId) {
                return false;
            }

            @Override
            public void blockActor(String actorId) {
                // ignore
            }

            @Override
            public void unblockActor(String actorId) {
                // ignore
            }

            @Override
            public Collection<ActorPreferences> getAllActorPreferences() {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public Collection<ExpirationPolicyConfig> getAllExpirationPolicies() {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public ActorPreferences getActorPreferences(String actorId) {
                throw new IllegalAccessError("Method not implemented");
            }
        };
    }

    @Override
    public InterruptedTasksService getInterruptedTasksService() {
        return new InterruptedTasksService() {
            @Override
            public void save(InterruptedTask itdTask) {
                // ignore
            }

            @Override
            public Collection<InterruptedTask> find(SearchCommand searchCommand) {
                throw new IllegalAccessError("Method not implemented");

            }

            @Override
            public Collection<InterruptedTask> findAll() {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public void delete(UUID processId, UUID taskId) {
                // ignore
            }

        };
    }

    @Override
    public GarbageCollectorService getGarbageCollectorService() {
        return new GarbageCollectorService() {
            @Override
            public void collect(UUID processId) {
                // ignore
            }

            @Override
            public int getCurrentSize() {
                return 0;
            }
        };
    }
}
