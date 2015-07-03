package ru.taskurotta.service.hz.adapter.schedule.tasks;

import ru.taskurotta.service.hz.adapter.schedule.HzJobStoreAdapter;

import java.io.Serializable;

/**
 * Javadoc should be here
 * Date: 10.12.13 19:21
 */
public class UpdateErrorCountRunnable implements Runnable, Serializable {

    private long id;
    private int count;
    private String message;

    public UpdateErrorCountRunnable(long id, int count, String message) {
        this.id = id;
        this.count = count;
        this.message = message;
    }

    @Override
    public void run() {
        HzJobStoreAdapter.getRealJobStore().updateErrorCount(id, count, message);
    }
}
