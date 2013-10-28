package ru.taskurotta.backend.statistics.datalistener.impl;

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
        private AbstractDataListener dataListener;

        public Metrics(String name, AbstractDataListener dataListener) {
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

    public void handle(String metricName, String datasetName, long count, double mean, long time) {
        super.handle(metricName, datasetName, count, mean, time);

        logger.trace("[{}]: find metrics for", metricName);

        Metrics metrics = metricsMap.get(metricName);

        logger.trace("[{}]: found metrics [{}]", metricName, metrics);

        if (metrics == null) {
            synchronized (metricsMap) {
                metrics = metricsMap.get(metricName);

                if (metrics == null) {
                    metrics = new Metrics(metricName, this);

                    logger.trace("[{}]: create metrics [{}]", metricName, metrics);

                    try {
                        mBeanServer.registerMBean(metrics, createName(metricName));
                    } catch (InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException e) {
                        logger.error("Catch exception while register MBean for [" + metricName + "]", e);
                    }

                    metricsMap.put(metricName, metrics);

                    logger.trace("[{}]: save metrics [{}]", metricName, metrics);
                }
            }
        }

        logger.trace("[{}]: update metrics [{}]", metricName, metrics);
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
