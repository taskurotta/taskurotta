package ru.taskurotta.bootstrap.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

public interface LoggingOutput {
    Appender<ILoggingEvent> build(LoggerContext context);
}
