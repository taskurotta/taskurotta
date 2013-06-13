package ru.taskurotta.backend.dependency;

import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:47 PM
 */
public interface DependencyBackend {


    /**
     * @return Dependency decision with tasks to start
     */
    public DependencyDecision applyDecision(DecisionContainer taskDecision);

    public void startProcess(TaskContainer task);
}
