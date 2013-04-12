package ru.taskurotta.backend.config.model;

import java.util.Properties;

public class ActorPreferences {

    private String id;
    private String type;
    private boolean blocked = false;
    private ExpirationPolicyConfig expirationPolicy;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public boolean isBlocked() {
        return blocked;
    }
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
    public ExpirationPolicyConfig getExpirationPolicy() {
        return expirationPolicy;
    }
    public void setExpirationPolicy(ExpirationPolicyConfig expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public class ExpirationPolicyConfig {

        private String className;
        private Properties properties;

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
            return "ExpirationPolicyConfig [className=" + className
                    + ", properties=" + properties + "]";
        }
    }

    @Override
    public String toString() {
        return "ActorPreferences [id=" + id + ", type=" + type + ", blocked="
                + blocked + ", expirationPolicy=" + expirationPolicy + "]";
    }

}
