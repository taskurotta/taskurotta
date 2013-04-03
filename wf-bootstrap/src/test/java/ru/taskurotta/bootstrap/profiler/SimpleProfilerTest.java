package ru.taskurotta.bootstrap.profiler;

import org.junit.BeforeClass;
import ru.taskurotta.bootstrap.TestWorker;

/**
 * User: stukushin
 * Date: 02.04.13
 * Time: 18:59
 */
public class SimpleProfilerTest extends ProfilerTest {

    @BeforeClass
    public static void setUp() throws Exception {
        profiler = new SimpleProfiler(TestWorker.class);
    }

}
