package ru.taskurotta.bootstrap.profiler;

import java.util.Properties;

import org.junit.BeforeClass;
import ru.taskurotta.bootstrap.TestWorker;

/**
 * User: romario, stukushin
 * Date: 3/22/13
 * Time: 2:52 PM
 */
public class MetricsProfilerTest extends ProfilerTest {

    @BeforeClass
    public static void setUp() throws Exception {
        profiler = new MetricsProfiler(TestWorker.class, new Properties());
    }
}
