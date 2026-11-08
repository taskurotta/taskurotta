package ru.taskurotta.dropwizard.resources.console.metrics;

import com.google.common.base.Optional;
import org.junit.Test;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * REST-ресурс /console/metrics: имена/дефолты параметров
 * (metric/type/zeroes-true/dataset/smooth-(-1)),
 * smooth/zeroes-фильтрация, пустой dataset → пустой ответ, ошибка провайдера → HTTP 500.
 */
public class MetricsResourceTest {

    // Фиксированные константы времени — детерминизм
    private static final long T0 = 1_000_000L;
    private static final long DT = 1000L;

    /** Стаб-провайдер: записывает полученные параметры, отдаёт заданный ответ или бросает заданный исключение. */
    private static final class RecordingProvider extends MetricsDataProvider {
        String metricName;
        List<String> dataSetNames;
        String scope;
        String dataType;
        String period;
        List<DatasetVO> toReturn = new ArrayList<DatasetVO>();
        RuntimeException toThrow;

        @Override
        public List<DatasetVO> getDataResponse(String metricName, List<String> dataSetNames, String scope,
                                               String dataType, String period) {
            this.metricName = metricName;
            this.dataSetNames = dataSetNames;
            this.scope = scope;
            this.dataType = dataType;
            this.period = period;
            if (toThrow != null) {
                throw toThrow;
            }
            return toReturn;
        }
    }

    private static DatasetVO datasetWith(List<Number[]> data) {
        DatasetVO ds = new DatasetVO();
        ds.setData(data);
        return ds;
    }

    @Test
    public void data_Defaults_Smooth_Zeroes_Error500() {
        final RecordingProvider provider = new RecordingProvider();
        MetricsResource resource = new MetricsResource();
        resource.setMetricsDataHandler(provider);

        // 1. Дефолты: все параметры не заданы → provider получает "-1" и пустой список датасетов
        List<DatasetVO> response = resource.getMetricsData(
                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                Optional.<String>absent(), Optional.<Boolean>absent(),
                Optional.<String>absent(), Optional.<Integer>absent());
        assertEquals("-1", provider.metricName);
        assertEquals("-1", provider.dataType);
        assertEquals("-1", provider.period);
        assertEquals("-1", provider.scope);
        assertTrue("пустой dataset → пустой список", provider.dataSetNames.isEmpty());
        assertTrue("пустой dataset → пустой ответ", response.isEmpty());

        // 2. Явные параметры проходят в provider как есть; dataset — список через запятую, отсортированный
        provider.toReturn = new ArrayList<DatasetVO>();
        response = resource.getMetricsData(
                Optional.of("rate"), Optional.of("1hour"), Optional.of("local"), Optional.of("poll"),
                Optional.of(Boolean.TRUE), Optional.of("b, a"), Optional.<Integer>absent());
        assertEquals("poll", provider.metricName);
        assertEquals("rate", provider.dataType);
        assertEquals("1hour", provider.period);
        assertEquals("local", provider.scope);
        assertEquals(Arrays.asList("a", "b"), provider.dataSetNames);

        // 3. smooth > 0 → сглаживание: 12 точек, шаг 6 → 2 точки
        provider.toReturn = new ArrayList<DatasetVO>();
        List<Number[]> raw = new ArrayList<Number[]>();
        for (int i = 0; i < 12; i++) {
            raw.add(new Number[]{T0 + i * DT, (long) (i + 1)});
        }
        provider.toReturn.add(datasetWith(raw));
        response = resource.getMetricsData(
                Optional.of("size"), Optional.of("1hour"), Optional.of("local"), Optional.of("queueSize"),
                Optional.of(Boolean.TRUE), Optional.of("someQueue"), Optional.of(6));
        assertEquals(1, response.size());
        List<Number[]> smoothed = response.get(0).getData();
        assertEquals(2, smoothed.size());
        assertEquals(T0 + 5 * DT, smoothed.get(0)[0].longValue());
        assertEquals(5L, smoothed.get(0)[1].longValue());
        assertEquals(T0 + 11 * DT, smoothed.get(1)[0].longValue());
        assertEquals(9L, smoothed.get(1)[1].longValue());

        // 4. zeroes = false → фильтрация: [null, null], [null, v] и отрицательные удаляются, 0 остаётся
        provider.toReturn = new ArrayList<DatasetVO>();
        List<Number[]> withGaps = new ArrayList<Number[]>(Arrays.asList(
                new Number[]{T0, 5L},
                new Number[]{T0 + DT, 0L},
                new Number[]{null, null},
                new Number[]{T0 + 3 * DT, -1L},
                new Number[]{T0 + 4 * DT, 7L}
        ));
        provider.toReturn.add(datasetWith(withGaps));
        response = resource.getMetricsData(
                Optional.of("size"), Optional.of("1hour"), Optional.of("local"), Optional.of("queueSize"),
                Optional.of(Boolean.FALSE), Optional.of("someQueue"), Optional.<Integer>absent());
        assertEquals(1, response.size());
        List<Number[]> filtered = response.get(0).getData();
        assertEquals(3, filtered.size());
        assertEquals(5L, filtered.get(0)[1].longValue());
        assertEquals(0L, filtered.get(1)[1].longValue());
        assertEquals(7L, filtered.get(2)[1].longValue());

        // 5. Любой Throwable провайдера → WebApplicationException HTTP 500
        provider.toThrow = new RuntimeException("provider failure");
        try {
            resource.getMetricsData(
                    Optional.of("rate"), Optional.of("1hour"), Optional.of("local"), Optional.of("poll"),
                    Optional.of(Boolean.TRUE), Optional.of("a"), Optional.<Integer>absent());
            fail("ожидался WebApplicationException");
        } catch (WebApplicationException expected) {
            assertEquals(500, expected.getResponse().getStatus());
        }
    }
}
