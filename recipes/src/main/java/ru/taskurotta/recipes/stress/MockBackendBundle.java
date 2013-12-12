package ru.taskurotta.recipes.stress;

import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.config.model.ExpirationPolicyConfig;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.gc.GarbageCollectorBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.BrokenProcessVO;
import ru.taskurotta.backend.process.SearchCommand;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.recipes.multiplier.MultiplierDecider;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorDefinition;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 11:10 AM
 */
public class MockBackendBundle implements BackendBundle {

    private static TaskContainer createRandomMultiplyTaskContainer() {
        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        return createRandomMultiplyTaskContainer(taskId, processId);
    }

    private static TaskContainer createRandomMultiplyTaskContainer(UUID taskId, UUID processId) {
        String actorId = ActorDefinition.valueOf(MultiplierDecider.class).getFullName();

        String a = Integer.toString(ThreadLocalRandom.current().nextInt());
        String b = Integer.toString(ThreadLocalRandom.current().nextInt());

        return new TaskContainer(taskId, processId, "multiply",
                actorId, TaskType.DECIDER_START, 0, 1, new ArgContainer[]{
                    new ArgContainer("java.lang.Integer", ArgContainer.ValueType.PLAIN, null, true, false, a),
                    new ArgContainer("java.lang.Integer", ArgContainer.ValueType.PLAIN, null, true, false, b)},
                new TaskOptionsContainer(new ArgType[] {ArgType.NONE, ArgType.NONE}), false, null);
    }

    @Override
    public ProcessBackend getProcessBackend() {
        return new ProcessBackend() {
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
        };
    }

    @Override
    public TaskBackend getTaskBackend() {
        return new TaskBackend() {
            @Override
            public void startProcess(TaskContainer taskContainer) {
                // ignore
            }

            @Override
            public TaskContainer getTaskToExecute(UUID taskId, UUID processId) {
                return createRandomMultiplyTaskContainer(taskId, processId);
            }

            @Override
            public TaskContainer getTask(UUID taskId, UUID processId) {
                return createRandomMultiplyTaskContainer(taskId, processId);
            }

            @Override
            public void addDecision(DecisionContainer taskDecision) {
                // ignore
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
        };
    }

    @Override
    public QueueBackend getQueueBackend() {
        return new QueueBackend() {

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

                counter ++;
                int currentMemMegaBytes = (int) (Runtime.getRuntime().freeMemory() / 1024 /1024);
                if (maxMemoMegabytes < currentMemMegaBytes) {
                    maxMemoMegabytes = currentMemMegaBytes;
                }

                if (counter % 5000 == 0) {
                    System.err.println("Mock queue backend poll: " + counter + " mem: " + maxMemoMegabytes);
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
                if (taskList != null) {
                    return actorId + "#"+ taskList;
                }

                return  actorId;
            }
        };
    }

    @Override
    public DependencyBackend getDependencyBackend() {
        return new DependencyBackend() {
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
    public ConfigBackend getConfigBackend() {
        return new ConfigBackend() {
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
    public BrokenProcessBackend getBrokenProcessBackend() {
        return new BrokenProcessBackend() {
            @Override
            public void save(BrokenProcessVO brokenProcessVO) {
                // ignore
            }

            @Override
            public Collection<BrokenProcessVO> find(SearchCommand searchCommand) {
                throw new IllegalAccessError("Method not implemented");

            }

            @Override
            public Collection<BrokenProcessVO> findAll() {
                throw new IllegalAccessError("Method not implemented");
            }

            @Override
            public void delete(UUID processId) {
                // ignore
            }

            @Override
            public void deleteCollection(Collection<UUID> processIds) {
                // ignore
            }
        };
    }

    @Override
    public GarbageCollectorBackend getGarbageCollectorBackend() {
        return new GarbageCollectorBackend() {
            @Override
            public void delete(UUID processId, String actorId) {
                // ignore
            }
        };
    }
}
