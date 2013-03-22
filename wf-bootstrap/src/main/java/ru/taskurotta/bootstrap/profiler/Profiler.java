package ru.taskurotta.bootstrap.profiler;

import ru.taskurotta.core.TaskTarget;

/**
 * User: romario
 * Date: 3/22/13
 * Time: 1:28 PM
 */
public interface Profiler {

    public void cycleStart();

    public void cycleFinish(boolean withTask, boolean withError);

    public void pullStart();

    public void pullFinish(boolean withTask);

    public void executeStart();

    public void executeFinish(TaskTarget taskTarget, boolean withError);

    public void releaseStart();

    public void releaseFinish();

    public void errorStart();

    public void errorFinish();


}
