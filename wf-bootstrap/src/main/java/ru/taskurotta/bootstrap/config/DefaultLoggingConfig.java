package ru.taskurotta.bootstrap.config;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import com.yammer.metrics.logback.InstrumentedAppender;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.taskurotta.bootstrap.logging.LoggingOutput;

/**
 * User: stukushin
 * Date: 27.03.13
 * Time: 17:49
 */
public class DefaultLoggingConfig implements LoggingConfig {

	private String className;
	private Properties properties;
	private Map<String, String> loggers;

	@Override
	public void init() {
		hijackJDKLogging();

		final Logger root = configureLevels();

		LoggingOutput output = null;
		try {
			output = (LoggingOutput) Class.forName(className).getConstructor(Properties.class).newInstance(properties);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		root.addAppender(output.build(root.getLoggerContext()));
	}

	private void hijackJDKLogging() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private Logger configureLevels() {
		final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.getLoggerContext().reset();

		final LevelChangePropagator propagator = new LevelChangePropagator();
		propagator.setContext(root.getLoggerContext());
		propagator.setResetJUL(true);

		root.getLoggerContext().addListener(propagator);

		if (loggers != null && !loggers.isEmpty()) {
			for (Map.Entry<String, String> entry : loggers.entrySet()) {
				((Logger) LoggerFactory.getLogger(entry.getKey())).setLevel(Level.toLevel(entry.getValue()));
			}
		}

		return root;
	}

	private void configureInstrumentation(Logger root) {
		final InstrumentedAppender appender = new InstrumentedAppender();
		appender.setContext(root.getLoggerContext());
		appender.start();
		root.addAppender(appender);
	}

	public void setClass(String className) {
		this.className = className;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void setLoggers(Map<String, String> loggers) {
		this.loggers = loggers;
	}
}
