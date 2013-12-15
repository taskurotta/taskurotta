package ru.taskurotta.service.statistics.metrics;

import ru.taskurotta.service.statistics.datalistener.DataListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * User: dimadin
 * Date: 12.09.13 17:20
 */
public class Metric {

    private final ConcurrentMap<String, CheckPoint> dataSets = new ConcurrentHashMap<>();
    private String name;

    public Metric(String name) {
        this.name = name;
    }

    public void mark(String dataSetName, long period) {

        CheckPoint dataSetCheckpoint = dataSets.get(dataSetName);
        if (dataSetCheckpoint == null) {

            synchronized (dataSets) {

                dataSetCheckpoint = dataSets.get(dataSetName);
                if (dataSetCheckpoint == null) {
                    dataSetCheckpoint = new CheckPoint();
                    dataSets.put(dataSetName, dataSetCheckpoint);
                }
            }
        }

        dataSetCheckpoint.mark(period);

    }

    public void dump(DataListener dataListener) {
        for (String dsName: dataSets.keySet()) {
            CheckPoint chp = dataSets.get(dsName);
            chp.dumpCurrentState(dataListener, name, dsName);
        }
    }


}
