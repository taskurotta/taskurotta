package ru.taskurotta.bootstrap.profiler.logback;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 27.03.13
 * Time: 13:38
 */
public class LoggerReporter extends AbstractPollingReporter implements MetricProcessor<StringBuilder> {

    private static final int CONSOLE_WIDTH = 80;

    public static void enable(long period, TimeUnit unit) {
        enable(Metrics.defaultRegistry(), period, unit);
    }

    public static void enable(MetricsRegistry metricsRegistry, long period, TimeUnit unit) {        
        final LoggerReporter reporter = new LoggerReporter(metricsRegistry,
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME),
                MetricPredicate.ALL);
        reporter.start(period, unit);
    }

    private final Logger logger;
    private final MetricPredicate predicate;
    private final Clock clock;
    private final TimeZone timeZone;
    private final Locale locale;

    public LoggerReporter(Logger logger) {
        this(Metrics.defaultRegistry(), logger, MetricPredicate.ALL);
    }

    public LoggerReporter(MetricsRegistry metricsRegistry, Logger logger, MetricPredicate predicate) {
        this(metricsRegistry, logger, predicate, Clock.defaultClock(), TimeZone.getDefault());
    }

    public LoggerReporter(MetricsRegistry metricsRegistry,
                           Logger logger,
                           MetricPredicate predicate,
                           Clock clock,
                           TimeZone timeZone) {
        this(metricsRegistry, logger, predicate, clock, timeZone, Locale.getDefault());
    }

    public LoggerReporter(MetricsRegistry metricsRegistry,
                           Logger logger,
                           MetricPredicate predicate,
                           Clock clock,
                           TimeZone timeZone, Locale locale) {
        super(metricsRegistry, "logger-reporter");
        this.logger = logger;
        this.predicate = predicate;
        this.clock = clock;
        this.timeZone = timeZone;
        this.locale = locale;
    }

    @Override
    public void run() {
        try {
            final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
            format.setTimeZone(timeZone);
            final String dateTime = format.format(new Date(clock.time()));

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(dateTime);
            stringBuilder.append(' ');
            for (int i = 0; i < (CONSOLE_WIDTH - dateTime.length() - 1); i++) {
                stringBuilder.append('=');
            }
            stringBuilder.append("\n");
            for (Map.Entry<String, SortedMap<MetricName, Metric>> entry : getMetricsRegistry().groupedMetrics(
                    predicate).entrySet()) {
                stringBuilder.append(entry.getKey());
                stringBuilder.append(":\n");
                for (Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
                    stringBuilder.append("  ");
                    stringBuilder.append(subEntry.getKey().getName());
                    stringBuilder.append(":\n");
                    subEntry.getValue().processWith(this, subEntry.getKey(), stringBuilder);
                    stringBuilder.append("\n");
                }
                stringBuilder.append("\n");
            }
            stringBuilder.append("\n");
            logger.info(stringBuilder.toString());
        } catch (Exception e) {
            logger.error("Error while write to log", e);
            e.printStackTrace();
        }
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, StringBuilder stringBuilder) {
        stringBuilder.append(String.format(locale, "    value = %s\n", gauge.value()));        
    }

    @Override
    public void processCounter(MetricName name, Counter counter, StringBuilder stringBuilder) {
        stringBuilder.append(String.format(locale, "    count = %d\n", counter.count()));
    }

    @Override
    public void processMeter(MetricName name, Metered meter, StringBuilder stringBuilder) {
        final String unit = abbrev(meter.rateUnit());
        stringBuilder.append(String.format(locale, "             count = %d\n", meter.count()));
        stringBuilder.append(String.format(locale, "         mean rate = %2.2f %s/%s\n",
                meter.meanRate(),
                meter.eventType(),
                unit));
        stringBuilder.append(String.format(locale, "     1-minute rate = %2.2f %s/%s\n",
                meter.oneMinuteRate(),
                meter.eventType(),
                unit));
        stringBuilder.append(String.format(locale, "     5-minute rate = %2.2f %s/%s\n",
                meter.fiveMinuteRate(),
                meter.eventType(),
                unit));
        stringBuilder.append(String.format(locale, "    15-minute rate = %2.2f %s/%s\n",
                meter.fifteenMinuteRate(),
                meter.eventType(),
                unit));
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, StringBuilder stringBuilder) {
        final Snapshot snapshot = histogram.getSnapshot();
        stringBuilder.append(String.format(locale, "               min = %2.2f\n", histogram.min()));
        stringBuilder.append(String.format(locale, "               max = %2.2f\n", histogram.max()));
        stringBuilder.append(String.format(locale, "              mean = %2.2f\n", histogram.mean()));
        stringBuilder.append(String.format(locale, "            stddev = %2.2f\n", histogram.stdDev()));
        stringBuilder.append(String.format(locale, "            median = %2.2f\n", snapshot.getMedian()));
        stringBuilder.append(String.format(locale, "              75%% <= %2.2f\n", snapshot.get75thPercentile()));
        stringBuilder.append(String.format(locale, "              95%% <= %2.2f\n", snapshot.get95thPercentile()));
        stringBuilder.append(String.format(locale, "              98%% <= %2.2f\n", snapshot.get98thPercentile()));
        stringBuilder.append(String.format(locale, "              99%% <= %2.2f\n", snapshot.get99thPercentile()));
        stringBuilder.append(String.format(locale, "            99.9%% <= %2.2f\n", snapshot.get999thPercentile()));
    }

    @Override
    public void processTimer(MetricName name, Timer timer, StringBuilder stringBuilder) {
        processMeter(name, timer, stringBuilder);
        final String durationUnit = abbrev(timer.durationUnit());
        final Snapshot snapshot = timer.getSnapshot();
        stringBuilder.append(String.format(locale, "               min = %2.2f%s\n", timer.min(), durationUnit));
        stringBuilder.append(String.format(locale, "               max = %2.2f%s\n", timer.max(), durationUnit));
        stringBuilder.append(String.format(locale, "              mean = %2.2f%s\n", timer.mean(), durationUnit));
        stringBuilder.append(String.format(locale, "            stddev = %2.2f%s\n", timer.stdDev(), durationUnit));
        stringBuilder.append(String.format(locale, "            median = %2.2f%s\n", snapshot.getMedian(), durationUnit));
        stringBuilder.append(String.format(locale, "              75%% <= %2.2f%s\n", snapshot.get75thPercentile(), durationUnit));
        stringBuilder.append(String.format(locale, "              95%% <= %2.2f%s\n", snapshot.get95thPercentile(), durationUnit));
        stringBuilder.append(String.format(locale, "              98%% <= %2.2f%s\n", snapshot.get98thPercentile(), durationUnit));
        stringBuilder.append(String.format(locale, "              99%% <= %2.2f%s\n", snapshot.get99thPercentile(), durationUnit));
        stringBuilder.append(String.format(locale, "            99.9%% <= %2.2f%s\n", snapshot.get999thPercentile(), durationUnit));
    }

    private String abbrev(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "us";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "m";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new IllegalArgumentException("Unrecognized TimeUnit: " + unit);
        }
    }
}
