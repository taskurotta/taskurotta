package ru.taskurotta.backend.dependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.Modification;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.ArgType;

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
    public DependencyDecision applyDecision(DecisionContainer taskDecision) {

        logger.debug("applyDecision() taskDecision = [{}]", taskDecision);

        UUID finishedTaskId = taskDecision.getTaskId();
        UUID processId = taskDecision.getProcessId();

        Modification modification = createLinksModification(taskDecision);
        Graph graph = null;

        boolean successfullySaved = false;

        for (int i = 0; i < retryTimes; i++) {

            graph = graphDao.getGraph(processId);

            UUID[] readyTasks = null;

            if (!graph.hasNotFinishedItem(finishedTaskId)) {
                readyTasks = graphDao.getReadyTasks(finishedTaskId);

                return new DependencyDecision().withReadyTasks(readyTasks);
            }


            graph.apply(modification);
            if (graphDao.updateGraph(graph)) {
                successfullySaved = true;
                break;
            }

            graph = graphDao.getGraph(processId);
        }

        if (!successfullySaved) {
            // TODO: should be analyzed at TaskServer
            return new DependencyDecision().withFail();
        }

        return new DependencyDecision().withReadyTasks(graph.getReadyItems());
    }

    @Override
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

        UUID completedTaskId = taskDecision.getTaskId();

        Modification modification = new Modification();
        modification.setCompletedItem(taskDecision.getTaskId());

        TaskContainer[] newTasks = taskDecision.getTasks();

        ArgContainer value = taskDecision.getValue();
        if (value != null && value.isPromise() && !value.isReady()) {
            modification.setWaitForAfterRelease(value.getTaskId());
        }

        if (newTasks == null) {
            return modification;
        }

        // - registration of all new tasks
        for (TaskContainer newTask : newTasks) {

            UUID childTaskId = newTask.getTaskId();

            modification.addNewItem(childTaskId);

            ArgContainer args[] = newTask.getArgs();
            if (args == null) {
                continue;
            }

            TaskOptionsContainer taskOptionsContainer = newTask.getOptions();
            ArgType[] argTypes = taskOptionsContainer != null ? taskOptionsContainer.getArgTypes() : null;

            for (int j = 0; j < args.length; j++) {
                ArgContainer arg = args[j];

                boolean isPromise = arg.isPromise();

                // skip not promises or resolved promises
                if (!isPromise || (isPromise && arg.isReady())) {
                    continue;
                }

                // skip @NoWait promises
                if (argTypes != null) {
                    if (ArgType.NO_WAIT.equals(argTypes[j])) {
                        continue;
                    }
                }

                modification.linkItem(childTaskId, arg.getTaskId());

            }
        }

        return modification;

    }

}