package ru.taskurotta.test.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created on 16.02.2015.
 */
public class JmxFinishedProcessCounter implements ProcessesCounter {

    private static final Logger logger = LoggerFactory.getLogger(JmxFinishedProcessCounter.class);

    private List<String> jmxServiceUrls;

    private List<MBeanServerConnection> connections = new ArrayList<>();

    private ExecutorService es = Executors.newFixedThreadPool(2);

    private void init() throws Exception {
        for (String jmxServiceUrl : jmxServiceUrls) {
            JMXServiceURL url = new JMXServiceURL(jmxServiceUrl);
            JMXConnector jmxc = JMXConnectorFactory.connect(url);
            connections.add(jmxc.getMBeanServerConnection());
        }
    }

    @Override
    public int getCount() {
        logger.info("Try to get data via JMX");
        try {
            int result = 0;
            List<Future<Long>> futures = new ArrayList<>();
            for (MBeanServerConnection mbsc : connections) {
                futures.add(es.submit(new GetCountCallable(mbsc)));
            }

            for (Future<Long> fResult: futures) {
                result += fResult.get(3, TimeUnit.SECONDS);
            }

            return result;
        } catch (Exception e) {
            logger.error("Cannot getFinishedCount", e);
            return 0;
        }
    }

    private static class GetCountCallable implements Callable<Long> {
        MBeanServerConnection mbsc;

        GetCountCallable(MBeanServerConnection mbsc) {
            this.mbsc = mbsc;
        }

        @Override
        public Long call() throws Exception {
            return Long.valueOf(mbsc.getAttribute(new ObjectName("fpCounter"), "Count").toString());
        }
    }

    public void setJmxServiceUrls(List<String> jmxServiceUrls) {
        this.jmxServiceUrls = jmxServiceUrls;
    }
}
