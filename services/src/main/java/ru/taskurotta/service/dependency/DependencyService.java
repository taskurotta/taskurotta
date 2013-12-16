package ru.taskurotta.service.dependency;

import ru.taskurotta.service.dependency.links.Graph;
import ru.taskurotta.service.dependency.links.GraphDao;
import ru.taskurotta.service.dependency.model.DependencyDecision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:47 PM
 */
public interface DependencyService {


    /**
     * @return Dependency decision with tasks to start
     */
    public DependencyDecision applyDecision(DecisionContainer taskDecision);

    public void startProcess(TaskContainer task);

    public Graph getGraph(UUID processId);

    public boolean changeGraph(GraphDao.Updater updater);
}
