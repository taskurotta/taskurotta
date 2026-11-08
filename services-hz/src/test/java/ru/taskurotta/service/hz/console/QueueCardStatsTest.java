package ru.taskurotta.service.hz.console;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Карточка очереди HzQueueStatTask:
 * in/out hour/day — точные total'ы enqueue/successfulPoll за час/сутки,
 * count — getLastValue(queueSize), lastActivity — время последней отметки poll.
 * Рецепт: HzQueueService — стаб mockito без запуска
 * конструктора (он требует HazelcastInstance); РЕАЛЬНЫЕ синглтоны
 * MetricsDataHandler/NumberDataHandler с init() в @BeforeClass; данные —
 * синтетические и детерминированные (фиксированные count/value и временные
 * константы, записанные вызовами handle/handleNumberData до чтения карточки).
 */
public class QueueCardStatsTest {

    // Фиксированные константы времени — детерминизм
    private static final long T0 = 1_000_000L;
    private static final long DT = 1000L;
    private static final long LAST_POLL_TIME = 2_000_000L;
    private static final long LAST_POLLED_TASK_ENQUEUE_TIME = 123456789L;

    private static final String QUEUE = "queueA";

    // Синтетические суммы: 61 отметка enqueue по 10 → час = 610.
    // Суточные строки пишутся на каждый тик (без «минутного переноса») →
    // сутки = все отметки: enqueue 610, successfulPoll 605.
    // 121 отметка successfulPoll по 5 → час = 605; сутки = 605.
    private static final long ENQUEUE_HOUR_TOTAL = 610L;
    private static final long ENQUEUE_DAY_TOTAL = 610L;
    private static final long SUCCESSFUL_POLL_HOUR_TOTAL = 605L;
    private static final long SUCCESSFUL_POLL_DAY_TOTAL = 605L;
    private static final int QUEUE_SIZE_LAST_VALUE = 17;

    @BeforeClass
    public static void initRealSingletons() {
        new MetricsDataHandler().init();
        new NumberDataHandler().init();
    }

    private static void feedMethodMetric(String metricName, int points, long countValue) {
        for (int i = 0; i < points; i++) {
            MetricsDataHandler.getInstance().handle(metricName, QUEUE, countValue, 1.0, T0 + i * DT);
        }
    }

    @Test
    public void queueCard_TotalsAndLastValue() throws Exception {
        // Синтетические данные — до чтения карточки
        feedMethodMetric(MetricName.ENQUEUE.getValue(), 61, 10);
        feedMethodMetric(MetricName.SUCCESSFUL_POLL.getValue(), 121, 5);
        MetricsDataHandler.getInstance().handle(MetricName.POLL.getValue(), QUEUE, 1, 2.5, LAST_POLL_TIME);
        NumberDataHandler.getInstance().handleNumberData(MetricName.QUEUE_SIZE.getValue(), QUEUE,
                Integer.valueOf(15), T0, 100);
        NumberDataHandler.getInstance().handleNumberData(MetricName.QUEUE_SIZE.getValue(), QUEUE,
                Integer.valueOf(QUEUE_SIZE_LAST_VALUE), T0 + 2 * DT, 100);

        HzQueueService queueService = mock(HzQueueService.class);
        when(queueService.getLastPolledTaskEnqueueTime(QUEUE)).thenReturn(LAST_POLLED_TASK_ENQUEUE_TIME);

        HzQueueStatTask task = new HzQueueStatTask(new ArrayList<String>(Arrays.asList(QUEUE)), "prefix");
        task.setHzQueueService(queueService);

        List<QueueStatVO> card = task.call();
        assertEquals(1, card.size());
        QueueStatVO item = card.get(0);
        assertEquals(QUEUE, item.getName());
        assertEquals(QUEUE_SIZE_LAST_VALUE, item.getCount());
        assertEquals(LAST_POLL_TIME, item.getLastActivity().getTime());
        assertEquals(ENQUEUE_HOUR_TOTAL, item.getInHour());
        assertEquals(SUCCESSFUL_POLL_HOUR_TOTAL, item.getOutHour());
        assertEquals(ENQUEUE_DAY_TOTAL, item.getInDay());
        assertEquals(SUCCESSFUL_POLL_DAY_TOTAL, item.getOutDay());
        assertEquals(LAST_POLLED_TASK_ENQUEUE_TIME, item.getLastPolledTaskEnqueueTime());
    }
}
