package ru.taskurotta.spring.configs;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.bootstrap.config.ProfilerConfig;
import ru.taskurotta.bootstrap.profiler.Profiler;

/**
 * User: stukushin
 * Date: 13.02.14
 * Time: 15:36
 */
public class ProfilerConfigPathXmlApplicationContext implements ProfilerConfig {

    private AbstractApplicationContext applicationContext;

    private String className;

    @Override
    public Profiler getProfiler(Class actorInterface) {
        Profiler profiler;

        try {
            profiler = (Profiler) applicationContext.getBean(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return profiler;
    }

    public void setClass(String className) {
        this.className = className;
    }

    public void setContext(String context) {
        this.applicationContext = new ClassPathXmlApplicationContext(new String[]{context}, false);
        applicationContext.refresh();
    }
}
