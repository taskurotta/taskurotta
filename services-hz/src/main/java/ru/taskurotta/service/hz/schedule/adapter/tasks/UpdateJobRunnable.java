package ru.taskurotta.service.hz.schedule.adapter.tasks;

import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.hz.schedule.adapter.HzJobStoreAdapter;

import java.io.Serializable;

/**
 * Javadoc should be here
 * Date: 10.12.13 19:18
 */
public class UpdateJobRunnable implements Runnable, Serializable {

    private JobVO jobVO;

    public UpdateJobRunnable(JobVO jobVO) {
        this.jobVO = jobVO;
    }

    @Override
    public void run() {
        HzJobStoreAdapter.getRealJobStore().update(jobVO, jobVO.getId());
    }
}
