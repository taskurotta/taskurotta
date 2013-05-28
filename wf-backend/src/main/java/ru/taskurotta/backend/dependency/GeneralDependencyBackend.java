package ru.taskurotta.backend.dependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Profiled;
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
 * User: romario
 * Date: 4/5/13
 * Time: 11:17 AM
 */
public class GeneralDependencyBackend implements DependencyBackend {

    private final static Logger logger = LoggerFactory.getLogger(GeneralDependencyBackend.class);

    private GraphDao graphDao;
    private int retryTimes;

    public GeneralDependencyBackend(GraphDao graphDao, int retryTimes) {

        this.graphDao = graphDao;
        this.retryTimes = retryTimes;
    }

    @Override
    @Profiled
    public DependencyDecision applyDecision(DecisionContainer taskDecision) {

        logger.debug("applyDecision() taskDecision = [{}]", taskDecision);

        UUID finishedTaskId = taskDecision.getTaskId();
        UUID processId = taskDecision.getProcessId();

        Modification modification = createLinksModification(taskDecision);
        Graph graph = null;

        boolean successfullySaved = false;

        for (int i = 0; i < retryTimes; i++) {

            graph = graphDao.getGraph(processId);

            if (!graph.hasNotFinishedItem(finishedTaskId)) {
                UUID[] readyTasks = graphDao.getReadyTasks(finishedTaskId);

                logger.warn("Won't apply graph modification");
                return new DependencyDecision().withReadyTasks(readyTasks);
            }


            graph.apply(modification);
            if (graphDao.updateGraph(graph)) {
                successfullySaved = true;
                break;
            }

        }

        if (!successfullySaved) {
            logger.warn("Can't apply graph modification");
            // TODO: should be analyzed at TaskServer
            return new DependencyDecision().withFail();
        }

        return new DependencyDecision().withReadyTasks(graph.getReadyItems());
    }

    @Override
    @Profiled
    public void startProcess(TaskContainer task) {
        logger.debug("startProcess() task = [{}]", task);

        graphDao.createGraph(task.getProcessId(), task.getTaskId());
    }


    /**
     * Convert taskDecision to modification view
     *
     * @param taskDecision
     * @return
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

}