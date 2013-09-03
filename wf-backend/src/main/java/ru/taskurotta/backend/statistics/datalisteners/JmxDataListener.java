package ru.taskurotta.backend.statistics.datalisteners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: stukushin
 * Date: 28.08.13
 * Time: 17:52
 */
public class JmxDataListener implements DataListener {

    private static final Logger logger = LoggerFactory.getLogger(JmxDataListener.class);

    private static final Map<String, Metrics> metricsMap = new ConcurrentHashMap<>();

    private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private static final String domain = "ru.taskurotta.metrics";

    private static final String separator = "#";

    public interface MetricsMBean {

        public String getType();

        public String getName();

        public String getActorId();

        public long getValue();

        public long getTime();
    }

    public class Metrics implements MetricsMBean {

        private String type;
        private String name;
        private String actorId;
        private long value;
        private long time;

        @Override
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getActorId() {
            return actorId;
        }

        public void setActorId(String actorId) {
            this.actorId = actorId;
        }

        @Override
        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        @Override
        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

    @Override
    public void handle(String type, String name, String actorId, long value, long time) {

        synchronized (metricsMap) {
            String key = type + "#" + name + "#" + actorId;

            Metrics metrics = metricsMap.get(key);

            if (metrics != null) {
                metrics.setType(type);
                metrics.setName(name);
                metrics.setActorId(actorId);
                metrics.setValue(value);
                metrics.setTime(time);

                return;
            }

            metrics = new Metrics();
            metrics.setType(type);
            metrics.setName(name);
            metrics.setActorId(actorId);
            metrics.setValue(value);
            metrics.setTime(time);

            try {
                mBeanServer.registerMBean(metrics, createName(type, name + separator + actorId));
            } catch (InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException e) {
                logger.error("Catch exception while register MBean for [" + name + "]", e);
            }

            metricsMap.put(key, metrics);
        }
    }

    private ObjectName createName(String type, String name) {
        String key = domain + "." + type;

        try {
            return new ObjectName(key, "name", name);
        } catch (MalformedObjectNameException e) {
            try {
                return new ObjectName(key, "name", ObjectName.quote(name));
            } catch (MalformedObjectNameException e1) {
                logger.warn("Unable to register [{}]", name, e1);
                throw new RuntimeException(e1);
            }
        }
    }
}
