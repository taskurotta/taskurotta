package ru.taskurotta.service.hz.adapter.schedule.tasks;

import ru.taskurotta.service.hz.adapter.schedule.HzJobStoreAdapter;

import java.io.Serializable;

/**
 * Javadoc should be here
 * Date: 10.12.13 19:11
 */
public class RemoveJobRunnable implements Runnable, Serializable {

    private long id;

    public RemoveJobRunnable(long id) {
        this.id = id;
    }

    @Override
    public void run() {
        HzJobStoreAdapter.getRealJobStore().remove(id);
    }

}
