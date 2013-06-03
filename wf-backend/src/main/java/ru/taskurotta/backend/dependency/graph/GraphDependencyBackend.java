package ru.taskurotta.backend.dependency.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
import ru.taskurotta.backend.dependency.DependencyBackend;
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
        DependencyDecision result = new DependencyDecision();
        if(released) {
            UUID[] readyTasks = taskNodeDao.getReadyTasks(taskDecision.getProcessId());
            result = result.withReadyTasks(readyTasks);
            if(readyTasks==null || readyTasks.length == 0) {
                boolean isProcessReady = taskNodeDao.isProcessReady(taskDecision.getProcessId());
                result.setFinishedProcessId(taskDecision.getProcessId());
            }
        }

        return result;
    }

    //is promise and have no Nowait annotation
    private static boolean isDepends(ArgContainer[] args, ArgType[] argTypes, int itemIndex) {
        boolean result = (args!=null && args[itemIndex].isPromise());
        if(result && argTypes!=null) {
            result = result && !ArgType.NO_WAIT.equals(argTypes[itemIndex]);
        }
        return result;
    }

    @Override
    public void startProcess(TaskContainer task) {
        TaskNode taskNode = createTaskNode(task);
        taskNodeDao.addNode(taskNode);
        logger.debug("Process start node added [{}]", taskNode);
    }

    //Creates nodes for argument dependencies
    private TaskNode createTaskNode(TaskContainer tc) {
        TaskNode newNode = new TaskNode(tc.getTaskId(), tc.getProcessId());
        ArgContainer[] args = tc.getArgs();
        ArgType[] argTypes = tc.getOptions()!=null? tc.getOptions().getArgTypes(): null;
        if(args!=null && args.length>0) {
            List<UUID> depends = new ArrayList<>();
            for(int i = 0; i<args.length; i++) {
                ArgContainer arg = args[i];
                if(isDepends(args, argTypes, i)) {
                    depends.add(arg.getTaskId());
                }

            }
            newNode.setDepends(depends);
            newNode.setType(tc.getType());
        }
        return newNode;
    }

    public void setTaskNodeDao(TaskNodeDao taskNodeDao) {
        this.taskNodeDao = taskNodeDao;
    }

}
