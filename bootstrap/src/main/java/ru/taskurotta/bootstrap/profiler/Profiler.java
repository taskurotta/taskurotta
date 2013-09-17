package ru.taskurotta.bootstrap.profiler;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.client.TaskSpreader;

/**
 * User: romario
 * Date: 3/22/13
 * Time: 1:28 PM
 */
public interface Profiler {

    public RuntimeProcessor decorate(RuntimeProcessor runtimeProcessor);

    public TaskSpreader decorate(TaskSpreader taskSpreader);

    public void cycleStart();

    public void cycleFinish();

}
