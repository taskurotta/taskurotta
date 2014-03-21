package ru.taskurotta.bootstrap.config;

import org.junit.Ignore;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.config.RuntimeConfig;

import java.util.Properties;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 3:54 PM
 */
@Ignore
public class TestRuntimeConfig implements RuntimeConfig {

    private String context;

    private Properties properties;


    @Override
    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RuntimeProcessor getRuntimeProcessor(Class actorInterface) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getContext() {
        return context;
    }
}
