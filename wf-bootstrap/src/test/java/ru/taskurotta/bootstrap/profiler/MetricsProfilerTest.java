package ru.taskurotta.bootstrap.profiler;

import org.junit.Test;
import ru.taskurotta.annotation.Worker;

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

        profiler.cycleFinish();
    }
}
