package ru.taskurotta.backend.dependency.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dependency backend implementation via process graph of separated TaskNode objects.
 *
 * Every applyDecision method call should use only one TaskNode object as
 * monitor(instead of using whole process's graph) providing possibility
 * of concurrent process graph modifications
 * User: dimadin
 * Date: 03.06.13 10:22
 */
public class GraphDependencyBackend implements DependencyBackend {

    private static final Logger logger = LoggerFactory.getLogger(GraphDependencyBackend.class);

    private TaskNodeDao taskNodeDao;

    public static GraphDependencyBackend getDefaultInstance() {
        GraphDependencyBackend gdb = new GraphDependencyBackend();
        gdb.setTaskNodeDao(new MemoryTaskNodeDao());
        return gdb;
    }

    @Override
    @Profiled
    public DependencyDecision applyDecision(DecisionContainer taskDecision) {
        logger.debug("applyDecision called with decision[{}]", taskDecision);

        //Append new created nodes if any
        TaskContainer[] createdTasks = taskDecision.getTasks();
        if(createdTasks!=null && createdTasks.length>0) {//have new tasks in dependencies -> creating new TaskNodes && updating dependencies
            for(TaskContainer task: createdTasks) {
                TaskNode newNode = createTaskNode(task);
                taskNodeDao.addNode(newNode);
            }
        }

        boolean released = taskNodeDao.releaseNode(taskDecision.getTaskId(), taskDecision.getProcessId());
        DependencyDecision result = new DependencyDecision(taskDecision.getProcessId());

        if(released) {
            UUID[] readyTasks = getReadyTasks(taskDecision.getProcessId());

            //There are new tasks, process is still running
            if(readyTasks!=null && readyTasks.length>0) {
                for(UUID readyTask: readyTasks) {
                    if(taskNodeDao.scheduleNode(readyTask, taskDecision.getProcessId())) {
                        result.addReadyTask(readyTask);
                    }
                }

            }

            boolean hasReadyTask = result.getReadyTasks()!=null && !result.getReadyTasks().isEmpty();

            if(!hasReadyTask && isProcessReady(taskDecision.getProcessId())) {
                result.setFinishedProcessId(taskDecision.getProcessId());
                result.setProcessFinished(true);
                int deleted = taskNodeDao.deleteProcessNodes(taskDecision.getProcessId());
                logger.debug("Deleted [{}] dependency nodes on process[{}] finish", deleted, taskDecision.getProcessId());
            }
        }

        return result;
    }

    private static boolean hasNowait(ArgType[] argTypes, int itemIndex) {
        return argTypes!=null && ArgType.NO_WAIT.equals(argTypes[itemIndex]);
    }

    @Override
    public void startProcess(TaskContainer task) {
        TaskNode taskNode = createTaskNode(task);
        taskNodeDao.addNode(taskNode);
        logger.debug("Process start node added [{}]", taskNode);
    }

    @Override
    public Graph getGraph(UUID processId) {
        return null;
    }

    //Creates nodes for argument dependencies
    private TaskNode createTaskNode(TaskContainer tc) {
        TaskNode newNode = new TaskNode(tc.getTaskId(), tc.getProcessId());
        ArgContainer[] args = tc.getArgs();
        ArgContainer[] waits = tc.getOptions()!=null? tc.getOptions().getPromisesWaitFor(): null;
        ArgType[] argTypes = tc.getOptions()!=null? tc.getOptions().getArgTypes(): null;
        List<UUID> depends = new ArrayList<>();

        if(args!=null && args.length>0) {
            for(int i = 0; i<args.length; i++) {
                ArgContainer arg = args[i];
                if(arg!=null && arg.isPromise()) {
                    if(!hasNowait(argTypes, i)) {
                        depends.add(arg.getTaskId());
                    }
                }
            }
        }

        if(waits!=null && waits.length>0) {
            for(ArgContainer wait: waits) {
                depends.add(wait.getTaskId());
            }
        }

        newNode.setDepends(depends);
        newNode.setType(tc.getType());

        return newNode;
    }

    private UUID[] getReadyTasks(UUID processId) {
        List<UUID> result = new ArrayList<>();
        List<TaskNode> nodes = taskNodeDao.getActiveProcessNodes(processId);

        for(TaskNode node: nodes) {
            boolean released = node.isReleased();
            boolean scheduled = node.isScheduled();
            boolean isAllDepReady = isAllDependenciesReady(nodes, node.getDepends());
            if(!(released||scheduled) && isAllDepReady) {
                logger.debug("Node[{}] released[{}], scheduled[{}], allDepReady[{}]", node.getId(), released, scheduled, isAllDepReady);
                result.add(node.getId());
            }
        }

        UUID[] resultAsArray = result.toArray(new UUID[result.size()]);
        logger.debug("Ready tasks for processId[{}] are[{}]", processId, resultAsArray);

        return resultAsArray;
    }

    private boolean isProcessReady(UUID processId) {
        List<TaskNode> nodes = taskNodeDao.getActiveProcessNodes(processId);
        return nodes==null || nodes.isEmpty();
    }

    private boolean isAllDependenciesReady(List<TaskNode> nodes, List<UUID> dependencyIds) {

        if(nodes!=null && !nodes.isEmpty() && dependencyIds!=null && !dependencyIds.isEmpty()) {
            for(TaskNode node: nodes) {
                if(dependencyIds.contains(node.getId())) {//is dependency node
                    if(!node.isReleased()) {
                        return false;
                    }
                }
            }
        }

        return true;//null or empty nodeIds list means AllReady==true
    }

    public void setTaskNodeDao(TaskNodeDao taskNodeDao) {
        this.taskNodeDao = taskNodeDao;
    }

    public TaskNodeDao getTaskNodeDao() {
        return taskNodeDao;
    }
}
