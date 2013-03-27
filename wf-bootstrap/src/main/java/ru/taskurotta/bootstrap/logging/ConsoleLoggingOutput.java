package ru.taskurotta.bootstrap.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.FilterAttachable;
import com.google.common.base.Strings;

import java.util.Properties;
import java.util.TimeZone;

public class ConsoleLoggingOutput implements LoggingOutput {

    private Level threshold = Level.ALL;
    private TimeZone timeZone = TimeZone.getDefault();
    private String logFormat;

    public ConsoleLoggingOutput(Properties properties) {
        this.threshold = properties.containsKey("threshold") ? Level.toLevel(String.valueOf(properties.get("threshold"))) : threshold;
        this.timeZone = properties.containsKey("timeZone") ? TimeZone.getTimeZone(String.valueOf(properties.get("timeZone"))) : timeZone;
        this.logFormat = properties.containsKey("logFormat") ? String.valueOf(properties.get("logFormat")).replaceAll("\\\\%", "%") : logFormat;
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context) {
        final LogFormatter formatter = new LogFormatter(context, timeZone);
        if (!Strings.isNullOrEmpty(logFormat)) {
            formatter.setPattern(logFormat);
        }
        formatter.start();

        final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setLayout(formatter);
        addThresholdFilter(appender, threshold);
        appender.start();

        return appender;
    }

    private void addThresholdFilter(FilterAttachable<ILoggingEvent> appender, Level threshold) {
        final ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel(threshold.toString());
        filter.start();
        appender.addFilter(filter);
    }
}
