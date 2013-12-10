package ru.taskurotta.schedule.adapter.tasks;

import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.adapter.HzJobStoreAdapter;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Javadoc should be here
 * Date: 10.12.13 19:02
 */
public class AddJobCallable implements Callable<Long>, Serializable {

    private JobVO jobVO;

    public AddJobCallable(JobVO jobVO) {
        this.jobVO = jobVO;
    }

    @Override
    public Long call() throws Exception {
        return HzJobStoreAdapter.getRealJobStore().addJob(jobVO);
    }

}
