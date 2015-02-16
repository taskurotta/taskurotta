package ru.taskurotta.bootstrap.config;

import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.util.PropertiesUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * User: stukushin
 * Date: 26.03.13
 * Time: 12:49
 */
public class DefaultProfilerConfig implements ProfilerConfig {

	private String className;
	private Properties properties;

	@Override
	public Profiler getProfiler(Class actorInterface) {

        properties = PropertiesUtil.mergeProperties(new Properties(), properties, null, "yaml");
        properties = PropertiesUtil.mergeProperties(properties, System.getProperties(), null, "system");

		Profiler profiler;

        try {
            profiler = (Profiler) Class.forName(className).getConstructor(Class.class, Properties.class).newInstance(actorInterface, properties);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return profiler;
	}

	public void setClass(String className) {
		this.className = className;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
