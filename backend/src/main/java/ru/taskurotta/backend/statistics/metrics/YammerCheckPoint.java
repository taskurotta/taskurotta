package ru.taskurotta.backend.statistics.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 05.09.13
 * Time: 14:42
 */
public class YammerCheckPoint implements CheckPoint {

    private String name;
    private String actorId;
    private DataListener dataListener;

    private Timer timer;

    public YammerCheckPoint(String name, String actorId, DataListener dataListener) {
        this.name = name;
        this.actorId = actorId;
        this.dataListener = dataListener;

        this.timer = Metrics.newTimer(CheckPoint.class, actorId, name);
    }

    @Override
    public void mark(long period) {
        timer.update(period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dump() {
        dataListener.handle(name, actorId, timer.count(), timer.mean(), System.currentTimeMillis());
        timer.clear();
    }
}
