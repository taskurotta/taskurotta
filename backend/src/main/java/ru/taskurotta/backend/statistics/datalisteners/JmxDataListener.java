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

        public String getName();

        public String getActorId();

        public int getCount();

        public double getValue();

        public long getTime();
    }

    public class Metrics implements MetricsMBean {

        private String name;
        private String actorId;
        private int count;
        private double value;
        private long time;

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
        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public double getValue() {
            return value;
        }

        public void setValue(double value) {
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
    public void handle(String name, String actorId, int count, double value, long time) {

        String key = name + separator + actorId;

        logger.trace("[{}]#[{}]: find metrics for [{}]", name, actorId, key);

        Metrics metrics = metricsMap.get(key);

        logger.trace("[{}]#[{}]: for [{}] found metrics [{}]", name, actorId, key, metrics);

        if (metrics == null) {
            synchronized (metricsMap) {
                metrics = metricsMap.get(key);

                if (metrics == null) {
                    metrics = new Metrics();

                    logger.trace("[{}]#[{}]: create metrics [{}]", name, actorId, metrics);

                    try {
                        mBeanServer.registerMBean(metrics, createName(key));
                    } catch (InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException e) {
                        logger.error("Catch exception while register MBean for [" + name + "]", e);
                    }

                    metricsMap.put(key, metrics);

                    logger.trace("[{}]#[{}]: save metrics [{}] for key [{}]", name, actorId, metrics, key);
                }
            }
        }

        metrics.setName(name);
        metrics.setActorId(actorId);
        metrics.setCount(count);
        metrics.setValue(value);
        metrics.setTime(time);

        logger.trace("[{}]#[{}]: update metrics [{}] for key [{}]", name, actorId, metrics, key);
    }

    private ObjectName createName(String name) {
        try {
            return new ObjectName(domain, "name", name);
        } catch (MalformedObjectNameException e) {
            try {
                return new ObjectName(domain, "name", ObjectName.quote(name));
            } catch (MalformedObjectNameException e1) {
                logger.warn("Unable to register [{}]", name, e1);
                throw new RuntimeException(e1);
            }
        }
    }
}
