package ru.taskurotta.schedule.adapter.tasks;

import ru.taskurotta.schedule.adapter.HzJobStoreAdapter;

import java.io.Serializable;

/**
 * Javadoc should be here
 * Date: 10.12.13 19:14
 */
public class UpdateJobStatusRunnable implements Runnable, Serializable {

    private long id;
    private int status;

    public UpdateJobStatusRunnable(long id, int status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public void run() {
        HzJobStoreAdapter.getRealJobStore().updateJobStatus(id, status);
    }

}
