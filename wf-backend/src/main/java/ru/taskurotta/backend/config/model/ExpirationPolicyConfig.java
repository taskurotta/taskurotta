package ru.taskurotta.backend.config.model;

import java.util.Properties;

public class ExpirationPolicyConfig {

    private String name;
    private String className;
    private Properties properties;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public Properties getProperties() {
        return properties;
    }
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "ExpirationPolicyConfig [name=" + name + ", className="
                + className + ", properties=" + properties + "]";
    }

}
