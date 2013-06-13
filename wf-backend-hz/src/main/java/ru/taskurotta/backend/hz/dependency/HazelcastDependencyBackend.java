package ru.taskurotta.backend.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 10.06.13
 * Time: 16:29
 */
public class HazelcastDependencyBackend implements DependencyBackend {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastDependencyBackend.class);

    private GraphDao graphDao;

    private IMap<UUID, Graph> processGraphMap;

    public static final String PROCESS_GRAPH_MAP = "processGraphMap";

    public HazelcastDependencyBackend(HazelcastInstance hazelcastInstance, GraphDao graphDao) {
        this.graphDao = graphDao;

        this.processGraphMap = hazelcastInstance.getMap(PROCESS_GRAPH_MAP);
    }

    @Override
    @Profiled
    public DependencyDecision applyDecision(DecisionContainer taskDecision) {
        logger.trace("Try apply task decision [{}]", taskDecision);

        UUID finishedTaskId = taskDecision.getTaskId();
        UUID processId = taskDecision.getProcessId();
        DependencyDecision resultDecision = new DependencyDecision(processId);

        Modification modification = createLinksModification(taskDecision);
        Graph graph = getGraph(processId);

        if (!graph.hasNotFinishedItem(finishedTaskId)) {
            UUID[] readyTasks = graphDao.getReadyTasks(finishedTaskId);

            logger.warn("Won't apply graph modification. Current task [{}] is already finished.", finishedTaskId);
            return resultDecision.withReadyTasks(readyTasks);
        }

        graph.apply(modification);
        if (graphDao.updateGraph(graph)) {
            resultDecision.setProcessFinished(graph.isFinished());
            return resultDecision.withReadyTasks(graph.getReadyItems());
        }

        logger.warn("Can't apply graph modification");
        // TODO: should be analyzed at TaskServer
        return resultDecision.withFail();
    }

    @Override
    @Profiled
    public void startProcess(TaskContainer task) {
        logger.debug("startProcess() task = [{}]", task);

        graphDao.createGraph(task.getProcessId(), task.getTaskId());

        getGraph(task.getProcessId());
    }

    /**
     * Convert taskDecision to modification view
     *
     * @param taskDecision - decision container, which should br apply to graph
     * @return modification for graph
     */
    private Modification createLinksModification(DecisionContainer taskDecision) {

        Modification modification = new Modification();
        modification.setCompletedItem(taskDecision.getTaskId());

        ArgContainer value = taskDecision.getValue();
        if (value != null && value.isPromise() && !value.isReady()) {
            modification.setWaitForAfterRelease(value.getTaskId());
        }

        TaskContainer[] newTasks = taskDecision.getTasks();
        if (newTasks != null) {
            for (TaskContainer newTask : newTasks) {
                registerNewTask(modification, newTask);
            }
        }

        return modification;
    }

    private void registerNewTask(Modification modification, TaskContainer newTask) {
        UUID childTaskId = newTask.getTaskId();

        modification.addNewItem(childTaskId);

        ArgContainer args[] = newTask.getArgs();
        if (args == null) {
            return;
        }

        TaskOptionsContainer taskOptionsContainer = newTask.getOptions();
        ArgType[] argTypes = taskOptionsContainer != null ? taskOptionsContainer.getArgTypes() : null;

        for (int j = 0; j < args.length; j++) {
            ArgContainer arg = args[j];

            if (arg.isPromise()) {
                // skip resolved promises
                if (arg.isReady()) {
                    continue;
                }

                // skip @NoWait promises
                if (argTypes != null && ArgType.NO_WAIT.equals(argTypes[j])) {
                    continue;
                }

                modification.linkItem(childTaskId, arg.getTaskId());

            } else if (arg.isObjectArray() && argTypes != null && ArgType.WAIT.equals(argTypes[j])) {

                processWaitArray(modification, childTaskId, arg);
            }
        }

        if (taskOptionsContainer != null && taskOptionsContainer.getPromisesWaitFor() != null) {
            ArgContainer[] promisesWaitForArgContainers = taskOptionsContainer.getPromisesWaitFor();

            for (ArgContainer argContainer : promisesWaitForArgContainers) {
                if (argContainer.isReady()) {
                    modification.linkItem(childTaskId, argContainer.getTaskId());
                }
            }
        }
    }

    private void processWaitArray(Modification modification, UUID childTaskId, ArgContainer parentArg) {
        ArgContainer[] innerValues = parentArg.getCompositeValue();
        for (ArgContainer arg : innerValues) {
            if (arg.isObjectArray()) {
                processWaitArray(modification, childTaskId, arg);
            } else if (arg.isPromise() && !arg.isReady()) {
                modification.linkItem(childTaskId, arg.getTaskId());
            }
        }
    }

    private synchronized Graph getGraph(UUID graphId) {
        Graph graph;

        if (processGraphMap.containsKey(graphId)) {
            graph = processGraphMap.get(graphId);
        } else {
            graph = graphDao.getGraph(graphId);
            processGraphMap.put(graphId, graph);
        }

        return graph;
    }
}
