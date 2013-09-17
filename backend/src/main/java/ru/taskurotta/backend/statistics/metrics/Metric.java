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
    private final CheckPoint mainDataSet = new CheckPoint();
    private String name;

    public Metric(String name) {
        this.name = name;
    }

    public void mark(String actorId, long period) {
        this.mainDataSet.mark(period);

        if (!dataSets.containsKey(actorId)) {
            synchronized (dataSets) {
                if (!dataSets.containsKey(actorId)) {
                    dataSets.put(actorId, new CheckPoint());
                }
            }
        }
        CheckPoint actorCheckpoint = dataSets.get(actorId);

        actorCheckpoint.mark(period);

    }

    public void dump(DataListener dataListener) {
        mainDataSet.dumpCurrentState(dataListener, name, name);
        for (String actorId: dataSets.keySet()) {
            CheckPoint chp = dataSets.get(actorId);
            chp.dumpCurrentState(dataListener, name, actorId);
        }
    }


}
