package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: dimadin
 * Date: 12.09.13 17:20
 */
public class Metric {

    private final Map<String, CheckPoint> dataSets = new ConcurrentHashMap<>();
    private String name;

    public Metric(String name) {
        this.name = name;
    }

    public void mark(String dataSetName, long period) {

        if (!dataSets.containsKey(dataSetName)) {
            synchronized (dataSets) {
                if (!dataSets.containsKey(dataSetName)) {
                    dataSets.put(dataSetName, new CheckPoint());
                }
            }
        }

        CheckPoint dataSetCheckpoint = dataSets.get(dataSetName);
        dataSetCheckpoint.mark(period);

    }

    public void dump(DataListener dataListener) {
        for (String dsName: dataSets.keySet()) {
            CheckPoint chp = dataSets.get(dsName);
            chp.dumpCurrentState(dataListener, name, dsName);
        }
    }


}
