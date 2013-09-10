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

    public interface MetricsMBean {

        public String getName();

        public long getCount();

        public double getValue();

        public long getTime();
    }

    public class Metrics implements MetricsMBean {

        private String name;
        private long count;
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
        public long getCount() {
            return count;
        }

        public void setCount(long count) {
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
    public void handle(String name, long count, double value, long time) {

        logger.trace("[{}]: find metrics for", name);

        Metrics metrics = metricsMap.get(name);

        logger.trace("[{}]: found metrics [{}]", name, metrics);

        if (metrics == null) {
            synchronized (metricsMap) {
                metrics = metricsMap.get(name);

                if (metrics == null) {
                    metrics = new Metrics();

                    logger.trace("[{}]: create metrics [{}]", name, metrics);

                    try {
                        mBeanServer.registerMBean(metrics, createName(name));
                    } catch (InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException e) {
                        logger.error("Catch exception while register MBean for [" + name + "]", e);
                    }

                    metricsMap.put(name, metrics);

                    logger.trace("[{}]: save metrics [{}]", name, metrics);
                }
            }
        }

        metrics.setName(name);
        metrics.setCount(count);
        metrics.setValue(value);
        metrics.setTime(time);

        logger.trace("[{}]: update metrics [{}]", name, metrics);
    }

    @Override
    public long[] getHourCount() {
        return new long[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long[] getDayCount() {
        return new long[0];  //To change body of implemented methods use File | Settings | File Templates.
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
