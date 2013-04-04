package ru.taskurotta.server.config;

import java.util.Properties;


/**
 * Configuration for every registered Actor, served by this TaskServer.
 */
public class ActorConfig {

    private String actorQueueId;

    private ExpirationPolicyConfig expirationPolicy;

    public String getActorQueueId() {
        return actorQueueId;
    }

    public void setActorQueueId(String actorQueueId) {
        this.actorQueueId = actorQueueId;
    }

    public ExpirationPolicyConfig getExpirationPolicy() {
        return expirationPolicy;
    }

    public void setExpirationPolicy(ExpirationPolicyConfig expirationPolicy) {
        this.expirationPolicy = expirationPolicy;
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
            return "ExpirationPolicy [className=" + className + ", properties="
                    + properties + "]";
        }
    }

    @Override
    public String toString() {
        return "ActorConfig [actorQueueId=" + actorQueueId
                + ", expirationPolicy=" + expirationPolicy + "]";
    }

}
