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
public class JmxDataListener extends AbstractDataListener {

    private static final Logger logger = LoggerFactory.getLogger(JmxDataListener.class);

    private static final Map<String, Metrics> metricsMap = new ConcurrentHashMap<>();

    private static final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private static final String domain = "ru.taskurotta.metrics";

    public interface MetricsMBean {

        public String getName();

        public long[] getHourCounts();

        public double[] getHourMeans();

        public long[] getDayCounts();

        public double[] getDayMeans();
    }

    public class Metrics implements MetricsMBean {

        private String name;
        private DataListener dataListener;

        public Metrics(String name, DataListener dataListener) {
            this.name = name;
            this.dataListener = dataListener;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long[] getHourCounts() {
            return dataListener.getHourCounts();
        }

        @Override
        public double[] getHourMeans() {
            return dataListener.getHourMeans();
        }

        @Override
        public long[] getDayCounts() {
            return dataListener.getDayCounts();
        }

        @Override
        public double[] getDayMeans() {
            return dataListener.getDayMeans();
        }
    }

    public void handle(String name, long count, double mean, long time) {
        super.handle(name, count, mean, time);

        logger.trace("[{}]: find metrics for", name);

        Metrics metrics = metricsMap.get(name);

        logger.trace("[{}]: found metrics [{}]", name, metrics);

        if (metrics == null) {
            synchronized (metricsMap) {
                metrics = metricsMap.get(name);

                if (metrics == null) {
                    metrics = new Metrics(name, this);

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

        logger.trace("[{}]: update metrics [{}]", name, metrics);
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
