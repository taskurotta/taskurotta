package ru.taskurotta.service.metrics;

import com.carrotsearch.sizeof.ObjectTree;
import com.carrotsearch.sizeof.RamUsageEstimator;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;

@Ignore
public class SizeOfTest {

    @Test
    public void VOTest() {

        MetricsDataHandler metricsDataHandler = new MetricsDataHandler();
        metricsDataHandler.init();

        NumberDataHandler numberDataHandler = new NumberDataHandler();
        numberDataHandler.init();

        MetricsFactory metricsFactory = new MetricsFactory(1, 3, metricsDataHandler, numberDataHandler);

        Object obj = metricsFactory;

        long size = getSize(obj, "Just created", 0);

        Metric simpleMetric = metricsFactory.getInstance("MetricName");

        size = getSize(obj, "Created first metric", size);

        Metric simpleMetric2 = metricsFactory.getInstance("MetricName2");

        size = getSize(obj, "Created second metric", size);

        Metric simpleMetric3 = metricsFactory.getInstance("MetricName3");

        size = getSize(obj, "Created third metric", size);

        simpleMetric.mark("dataSet1", 10l);

        size = getSize(obj, "After first mark dataSet1", size);

        simpleMetric.mark("dataSet2", 10l);

        size = getSize(obj, "After first mark dataSet2", size);

        for (int i = 0; i < 60 * 60 * 100; i++) {
            metricsDataHandler.handle("MetricName", "dataSet1", 100l, 100D, i);
        }

        size = getSize(obj, "After 100 hours MetricName->dataSet1", size);

        for (int i = 0; i < 60 * 60 * 100; i++) {
            metricsDataHandler.handle("MetricName", "dataSet2", 100l, 100D, i);
        }

        size = getSize(obj, "After 100 hours MetricName->dataSet2", size);

        /**
         * One metric dataSet allocates 428 kilobytes of memory.
         *
         * Global metrics:
         * 1. GC queue size
         * 2. OperationExecutor queue size (recovery use it)
         * 3. Free memory
         * 4. Total memory
         * 5. Start process
         * 6. Poll task
         * 7. Success Poll
         * 8. Release
         * 9. Release execution time
         * 10. Release error
         *
         * Sum is 428 * 10 / 1024 = 4.180 Megabytes
         *
         * Per Actor (Actor + taskList:
         * 1. Start process
         * 2. Poll
         * 3. Success Poll
         * 4. Release
         * 5. Release execution time
         * 6. Release error
         *
         * One actor metrics allocate in sum: 428 * 6 / 1024 = 2.507 Megabytes
         *
         * For example: 30 actors allocate 79.39 Megabytes after 24 hours of continuous work
         */
    }

    @Test
    public void VOTestMem() throws InterruptedException {

        MetricsDataHandler metricsDataHandler = new MetricsDataHandler();
        metricsDataHandler.init();

        for (int j = 1; j < 10000; j ++) {

            String dataSetName = "" + j;

            System.err.println("Start " + j);
            for (int i = 0; i < 60 * 60 * 100; i++) {
                metricsDataHandler.handle("MetricName", dataSetName, 100l, 100D, i);
            }

            System.err.println("Done " + j + " free mem = " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " " +
                    "mb");
//            TimeUnit.SECONDS.sleep(1);
        }

    }

    private long getSize(Object obj, String msg, long oldSize) {
        return getSize(obj, msg, oldSize, false);
    }

    private long getSize(Object obj, String msg, long oldSize, boolean dumpTree) {
        long size = RamUsageEstimator.sizeOf(obj);
        System.err.println(msg + ": " + "size = " + RamUsageEstimator.sizeOf(obj) + " delta is " + (size - oldSize));

        if (dumpTree) {
            System.err.println(ObjectTree.dump(obj));
        }

        return size;
    }
}
