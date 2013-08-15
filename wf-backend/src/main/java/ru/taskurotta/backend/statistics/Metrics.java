package ru.taskurotta.backend.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: romario
 * Date: 8/14/13
 * Time: 7:34 PM
 */
public class Metrics {

    private Map<String, CheckPoint> checkPoints = new ConcurrentHashMap();

    private int dataLength = 500;

    private DataListener dataListener = new LoggerDataListener();

    public class CheckPoint {

        private int counter = 0;
        private long[] data = new long[dataLength];

        private String name;

        private CheckPoint(String name) {
            this.name = name;
        }

        public void mark(long startPointTimeMilliseconds) {

            long metricData = System.currentTimeMillis() - startPointTimeMilliseconds;

            if (metricData < 0) {
                metricData = 0;
            }

            int localCounter = counter;
            int pointer = counter++ % dataLength;

            data[pointer] = metricData;

            if (pointer == 0 && localCounter > 0) {
                dataListener.handle(name, getAverageValue());
            }
        }


        private long getAverageValue() {

            long sum = 0;

            for (long metricData : data) {
                sum += metricData;
            }

            return (int) sum / dataLength;
        }
    }


    public static interface DataListener {

        public void handle(String metricName, long averageDataMetric);
    }

    public static class LoggerDataListener implements DataListener {

        private final static Logger logger = LoggerFactory.getLogger(LoggerDataListener.class);

        @Override
        public void handle(String metricName, long averageDataMetric) {

            if (logger.isDebugEnabled()) {
                logger.debug("Average metric [{}] is [{}]", metricName, averageDataMetric);
            }
        }

    }


    public Metrics() {
    }

    public Metrics(int dataLength) {
        this.dataLength = dataLength;
    }

    public CheckPoint create(String name) {
        CheckPoint checkPoint = checkPoints.get(name);

        if (checkPoint != null) {
            return checkPoint;
        }

        synchronized (this) {
            checkPoint = checkPoints.get(name);

            if (checkPoint != null) {
                return checkPoint;
            }

            checkPoint = new CheckPoint(name);
            checkPoints.put(name, checkPoint);
        }

        return checkPoint;
    }

    public void setDataListener(DataListener dataListener) {

        this.dataListener = dataListener;
    }
}
