package ru.taskurotta.bootstrap.logging;

import java.util.Properties;
import java.util.TimeZone;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterAttachable;
import com.google.common.base.Strings;


public class FileLoggingOutput implements LoggingOutput {

	private Level threshold = Level.ALL;
	private String currentLogFilename = "./logs/output.log";
	private boolean archive = true;
	private String archivedLogFilenamePattern = "./logs/output-%d.log.gz";
	private int archivedFileCount = 5;
	private TimeZone timeZone = TimeZone.getDefault();
	private String logFormat;

	public FileLoggingOutput(Properties properties) {
		this.threshold = properties.containsKey("threshold") ? Level.toLevel(String.valueOf(properties.get("threshold"))) : threshold;
		this.currentLogFilename = properties.containsKey("currentLogFilename") ? String.valueOf(properties.get("currentLogFilename")) : currentLogFilename;
		this.archive = properties.containsKey("archive") ? Boolean.parseBoolean(String.valueOf(properties.get("archive"))) : archive;
		this.archivedLogFilenamePattern = properties.containsKey("archivedLogFilenamePattern") ? String.valueOf(properties.get("archivedLogFilenamePattern")) : archivedLogFilenamePattern;
		this.archivedFileCount = properties.containsKey("archivedFileCount") ? Integer.parseInt(String.valueOf(properties.get("archivedFileCount"))) : archivedFileCount;
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

		final FileAppender<ILoggingEvent> appender = archive ?
				new RollingFileAppender<ILoggingEvent>() :
				new FileAppender<ILoggingEvent>();

		appender.setAppend(true);
		appender.setContext(context);
		appender.setLayout(formatter);
		appender.setFile(currentLogFilename);
		appender.setPrudent(false);

		addThresholdFilter(appender, threshold);

		if (archive) {
			final DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> triggeringPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent>();
			triggeringPolicy.setContext(context);

			final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
			rollingPolicy.setContext(context);
			rollingPolicy.setFileNamePattern(archivedLogFilenamePattern);
			rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(
					triggeringPolicy);
			triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
			rollingPolicy.setMaxHistory(archivedFileCount);

			((RollingFileAppender<ILoggingEvent>) appender).setRollingPolicy(rollingPolicy);
			((RollingFileAppender<ILoggingEvent>) appender).setTriggeringPolicy(triggeringPolicy);

			rollingPolicy.setParent(appender);
			rollingPolicy.start();
		}

		appender.stop();
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
