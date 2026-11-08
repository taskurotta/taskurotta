package ru.taskurotta.server;

import org.junit.Test;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.service.metrics.MetricsFactory;
import ru.taskurotta.service.metrics.handler.DataListener;
import ru.taskurotta.service.metrics.handler.NumberDataListener;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Wire-контракт интеграции: после startProcess/poll/release в хранилище
 * метрик видны ключи «метрика#актёр»
 * и «метрика#метрика» (двойная отметка); successfulPoll — только если poll вернул
 * задачу; errorDecision — только при containsError.
 * Реальный MetricsFactory с периодом дампа 1 с (initial delay 0): сборщик
 * накапливает итоги по нескольким дамп-циклам — первичный дамп (delay 0) может
 * забрать частичное состояние, латч открывается, когда все ожидаемые пары достигли
 * ожидаемых итогов (таймаут 10 с, без sleeps «на глаз»).
 */
public class MetricsTaskServerTest {

    private static final String ACTOR_ID = "actorA#1";
    private static final long EXEC_TIME_OK = 12345L;
    private static final long EXEC_TIME_FAILED = 678L;

    @Worker(name = "testWorker")
    private static final class TestActor {
    }

    @Worker(name = "testWorker2")
    private static final class TestActor2 {
    }

    /** Ручной стаб TaskServer: фиксирует вызовы, poll возвращает заданное значение. */
    private static final class RecordingTaskServer implements TaskServer {
        int startProcessCalls;
        int pollCalls;
        int releaseCalls;
        TaskContainer pollResult;
        boolean timeoutCalled;
        long timeoutValue;

        @Override
        public void startProcess(TaskContainer task) {
            startProcessCalls++;
        }

        @Override
        public TaskContainer poll(ActorDefinition actorDefinition) {
            pollCalls++;
            return pollResult;
        }

        @Override
        public void release(DecisionContainer taskResult) {
            releaseCalls++;
        }

        @Override
        public void updateTaskTimeout(UUID taskId, UUID processId, long timeout) {
            timeoutCalled = true;
            timeoutValue = timeout;
        }
    }

    /** NumberDataListener без действия: периодическая метрика memory не мешает методическим ассертам. */
    private static final class NoopNumberListener implements NumberDataListener {
        @Override
        public void handleNumberData(String metricName, String datasetName, Number value, long currentTime,
                                     int maxPoints) {
        }
    }

    /**
     * Накопитель дамп-циклов: (метрика, ключ) → суммарный count и сумма (count×mean).
     * Латч открывается, когда все ожидаемые пары достигли ожидаемых итогов.
     */
    private static final class DumpCollector implements DataListener {
        private final Map<String, long[]> counts = new ConcurrentHashMap<String, long[]>();
        private final Map<String, double[]> sums = new ConcurrentHashMap<String, double[]>();
        private final Map<String, Long> expectedCounts;
        private final CountDownLatch latch = new CountDownLatch(1);

        DumpCollector(Map<String, Long> expectedCounts) {
            this.expectedCounts = expectedCounts;
        }

        @Override
        public void handle(String metricName, String datasetName, long count, double mean, long currentTime) {
            String key = metricName + "#" + datasetName;
            synchronized (counts) {
                long[] c = counts.get(key);
                if (c == null) {
                    c = new long[1];
                    counts.put(key, c);
                }
                c[0] += count;
                double[] s = sums.get(key);
                if (s == null) {
                    s = new double[2];
                    sums.put(key, s);
                }
                s[0] += count;
                if (count > 0) {
                    // count == 0 → mean NaN (idle), в сумму не участвует
                    s[1] += count * mean;
                }
            }
            if (allExpectedReached()) {
                latch.countDown();
            }
        }

        private boolean allExpectedReached() {
            for (Map.Entry<String, Long> e : expectedCounts.entrySet()) {
                long[] c = counts.get(e.getKey());
                if (c == null || c[0] < e.getValue()) {
                    return false;
                }
            }
            return true;
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        long countOf(String metric, String key) {
            long[] c = counts.get(metric + "#" + key);
            return c == null ? 0 : c[0];
        }

        double meanOf(String metric, String key) {
            double[] s = sums.get(metric + "#" + key);
            if (s == null || s[0] == 0) {
                return Double.NaN;
            }
            return s[1] / s[0];
        }

        boolean seen(String metric, String key) {
            return counts.containsKey(metric + "#" + key);
        }
    }

    @Test
    public void doubleMark_ActorAndGeneral_Conditional() throws Exception {
        ActorDefinition actorA = ActorDefinition.valueOf(TestActor.class);
        ActorDefinition actorB = ActorDefinition.valueOf(TestActor2.class);
        String actorKeyA = ActorUtils.getFullActorName(actorA);
        String actorKeyB = ActorUtils.getFullActorName(actorB);

        Map<String, Long> expected = new HashMap<String, Long>();
        expected.put("startProcess#" + ACTOR_ID, 1L);
        expected.put("startProcess#startProcess", 1L);
        expected.put("poll#" + actorKeyA, 1L);
        expected.put("poll#" + actorKeyB, 1L);
        expected.put("poll#poll", 2L);
        expected.put("successfulPoll#" + actorKeyA, 1L);
        expected.put("successfulPoll#successfulPoll", 1L);
        expected.put("release#" + ACTOR_ID, 2L);
        expected.put("release#release", 2L);
        expected.put("executionTime#" + ACTOR_ID, 2L);
        expected.put("executionTime#executionTime", 2L);
        expected.put("errorDecision#" + ACTOR_ID, 1L);
        expected.put("errorDecision#errorDecision", 1L);

        final DumpCollector collector = new DumpCollector(expected);
        final RecordingTaskServer taskServer = new RecordingTaskServer();
        // Реальный завод: период дампа 1 с; период memory — 3600 с, её первичная отметка
        // идёт в отдельный NumberDataListener и методические ассерты не трогает
        final MetricsFactory factory = new MetricsFactory(1, 2, collector, new NoopNumberListener());
        try {
            final MetricsTaskServer server = new MetricsTaskServer(taskServer, factory, 3600);

            final UUID taskId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            final UUID processId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            final TaskContainer task = new TaskContainer(taskId, processId, taskId, "doWork",
                    ACTOR_ID, null, 0, 0, null, null, false, null);

            server.startProcess(task);

            taskServer.pollResult = task;
            server.poll(actorA);

            taskServer.pollResult = null;
            server.poll(actorB);

            server.release(new DecisionContainer(taskId, processId, taskId, null, null, 0, null,
                    ACTOR_ID, EXEC_TIME_OK));
            server.release(new DecisionContainer(taskId, processId, taskId, null, new ErrorContainer(), 0,
                    null, ACTOR_ID, EXEC_TIME_FAILED));

            server.updateTaskTimeout(taskId, processId, 1234L);

            assertTrue("дампы не доставили все отметки за 10 с", collector.await(10, TimeUnit.SECONDS));

            // двойная отметка: ключ «метрика#актёр» и «метрика#метрика»
            assertEquals(1, collector.countOf("startProcess", ACTOR_ID));
            assertEquals(1, collector.countOf("startProcess", "startProcess"));
            assertEquals(1, collector.countOf("poll", actorKeyA));
            assertEquals(1, collector.countOf("poll", actorKeyB));
            assertEquals(2, collector.countOf("poll", "poll"));

            // successfulPoll — только если poll вернул задачу: актёр B без задачи не отмечен
            assertEquals(1, collector.countOf("successfulPoll", actorKeyA));
            assertEquals(1, collector.countOf("successfulPoll", "successfulPoll"));
            assertFalse("у актёра B нет задачи — отметки successfulPoll быть не должно",
                    collector.seen("successfulPoll", actorKeyB));

            // release и executionTime — оба релиза, среднее по executionTime
            assertEquals(2, collector.countOf("release", ACTOR_ID));
            assertEquals(2, collector.countOf("release", "release"));
            assertEquals(2, collector.countOf("executionTime", ACTOR_ID));
            assertEquals((EXEC_TIME_OK + EXEC_TIME_FAILED) / 2.0,
                    collector.meanOf("executionTime", ACTOR_ID), 1e-9);

            // errorDecision — только при containsError: ровно один (один сбойный релиз)
            assertEquals(1, collector.countOf("errorDecision", ACTOR_ID));
            assertEquals(1, collector.countOf("errorDecision", "errorDecision"));

            // декоратор пересылает всё в обёрнутый сервер
            assertEquals(1, taskServer.startProcessCalls);
            assertEquals(2, taskServer.pollCalls);
            assertEquals(2, taskServer.releaseCalls);
            assertTrue(taskServer.timeoutCalled);
            assertEquals(1234L, taskServer.timeoutValue);
        } finally {
            factory.shutdown();
        }
    }
}
