package ru.taskurotta.bootstrap.profiler;

import org.junit.Test;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

/**
 * User: romario
 * Date: 3/22/13
 * Time: 2:52 PM
 */
public class MetricsProfilerTest {

    @Worker
    public static class SimpleWorker {

    }

    @Test
    public void test() {

        Profiler profiler = new MetricsProfiler(SimpleWorker.class);

        profiler.cycleStart();

        profiler.pullStart();
        profiler.pullFinish(true);

        profiler.executeStart();
        profiler.executeFinish(null, false);

        profiler.releaseStart();
        profiler.releaseFinish();

        profiler.cycleFinish(true, false);
    }
}
