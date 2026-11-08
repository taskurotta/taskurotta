package ru.taskurotta.dropwizard.resources.console.metrics;

import org.junit.Test;
import ru.taskurotta.dropwizard.resources.console.metrics.support.OptionsSupportBean;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.AvailableOptionsVO;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.OptionVO;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.service.metrics.handler.DatasetSummary;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Форма /console/metrics/options: список метрик и их датасетов;
 * OptionVO.general = true, если имя датасета совпадает с именем метрики
 * (конвенция двойной отметки); дефолты типов/периодов/скоупов.
 */
public class MetricsOptionsProviderTest {

    private static final class StubMethodRetriever implements MetricsMethodDataRetriever {
        @Override
        public Collection<String> getMetricNames() {
            return Arrays.asList("poll");
        }

        @Override
        public Collection<String> getDataSetsNames(String metricName) {
            // "poll" — агрегатный датасет (имя = имя метрики), "actorA" — обычный
            return Arrays.asList("poll", "actorA");
        }

        @Override
        public long getTotalCountOfLastHour(String metricName, String datasetName) {
            return 0;
        }

        @Override
        public long getTotalCountOfLastDay(String metricName, String datasetName) {
            return 0;
        }

        @Override
        public DataPointVO<Long>[] getCountsForLastHour(String metricName, String datasetName) {
            return new DataPointVO[0];
        }

        @Override
        public DataPointVO<Long>[] getCountsForLastDay(String metricName, String datasetName) {
            return new DataPointVO[0];
        }

        @Override
        public DataPointVO<Double>[] getMeansForLastHour(String metricName, String datasetName) {
            return new DataPointVO[0];
        }

        @Override
        public DataPointVO<Double>[] getMeansForLastDay(String metricName, String datasetName) {
            return new DataPointVO[0];
        }

        @Override
        public Date getLastActivityTime(String metricName, String datasetName) {
            return null;
        }

        @Override
        public long getLastActivityTimeMillis(String metricName, String datasetName) {
            return 0;
        }

        @Override
        public DatasetSummary getDatasetSummary(String metricName, String datasetName) {
            return null;
        }
    }

    private static final class StubNumberRetriever implements MetricsNumberDataRetriever {
        @Override
        public Collection<String> getNumberMetricNames() {
            return Arrays.asList("queueSize");
        }

        @Override
        public Collection<String> getNumberDataSets(String metricName) {
            // "queueSize" — агрегатный (имя = имя метрики), "queueA" — обычный
            return Arrays.asList("queueA", "queueSize");
        }

        @Override
        public DataPointVO<Number>[] getData(String metricName, String datasetName) {
            return new DataPointVO[0];
        }

        @Override
        public Date getLastActivityTime(String metricName, String datasetName) {
            return null;
        }

        @Override
        public Number getLastValue(String metricName, String datasetName) {
            return null;
        }
    }

    private static List<String> optionValues(List<OptionVO> options) {
        List<String> result = new java.util.ArrayList<String>();
        for (OptionVO option : options) {
            result.add(option.getValue());
        }
        return result;
    }

    @Test
    public void availableOptions_ShapeGeneralFlag() {
        MetricsOptionsProvider provider = new MetricsOptionsProvider();
        provider.setMethodDataRetriever(new StubMethodRetriever());
        provider.setNumberDataRetriever(new StubNumberRetriever());

        OptionsSupportBean support = new OptionsSupportBean();
        Map<String, String> metricsDescription = new HashMap<String, String>();
        metricsDescription.put("poll", "Poll operations");
        metricsDescription.put("queueSize", "Queue sizes");
        support.setMetricsDescription(metricsDescription);
        // Дефолты — как в spring-конфиге консоли
        support.setDefaultDataTypes("rate, mean");
        support.setDefaultPeriods("1hour, 24hours");
        support.setDefaultScopes("local");
        // Числовая метрика — свои типы/периоды/скоупы
        Map<String, String> dataTypes = new HashMap<String, String>();
        dataTypes.put("queueSize", "size");
        support.setMetricDataTypes(dataTypes);
        Map<String, String> periods = new HashMap<String, String>();
        periods.put("queueSize", "24hours, 1hour, 5minutes, 1minute");
        support.setMetricPeriods(periods);
        Map<String, String> scopes = new HashMap<String, String>();
        scopes.put("queueSize", "cluster");
        support.setMetricScopes(scopes);
        provider.setOptionsSupportBean(support);

        AvailableOptionsVO options = provider.getAvailableOptions();

        // metrics: числовые + методические, name — описание из OptionsSupportBean
        assertEquals(2, options.getMetrics().size());
        assertEquals("queueSize", options.getMetrics().get(0).getValue());
        assertEquals("Queue sizes", options.getMetrics().get(0).getName());
        assertEquals("poll", options.getMetrics().get(1).getValue());
        assertEquals("Poll operations", options.getMetrics().get(1).getName());

        // dataSets: map метрика → датасеты; general = true только у датасета с именем метрики
        assertEquals(2, options.getDataSets().size());
        List<OptionVO> pollDataSets = options.getDataSets().get("poll");
        assertEquals(Arrays.asList("poll", "actorA"), optionValues(pollDataSets));
        assertTrue(pollDataSets.get(0).isGeneral());
        assertFalse(pollDataSets.get(1).isGeneral());
        List<OptionVO> queueDataSets = options.getDataSets().get("queueSize");
        assertEquals(Arrays.asList("queueA", "queueSize"), optionValues(queueDataSets));
        assertFalse(queueDataSets.get(0).isGeneral());
        assertTrue(queueDataSets.get(1).isGeneral());

        // dataTypes: методическая — дефолт «rate, mean»; числовая — «size» из карты
        assertEquals(Arrays.asList("rate", "mean"), optionValues(options.getDataTypes().get("poll")));
        assertEquals(Collections.singletonList("size"), optionValues(options.getDataTypes().get("queueSize")));

        // periods: методическая — дефолт «1hour, 24hours»; числовая — из карты
        assertEquals(Arrays.asList("1hour", "24hours"), optionValues(options.getPeriods().get("poll")));
        assertEquals(Arrays.asList("24hours", "1hour", "5minutes", "1minute"),
                optionValues(options.getPeriods().get("queueSize")));

        // scopes: методическая — дефолт «local»; числовая — «cluster» из карты
        assertEquals(Collections.singletonList("local"), optionValues(options.getScopes().get("poll")));
        assertEquals(Collections.singletonList("cluster"), optionValues(options.getScopes().get("queueSize")));
    }
}
