package ru.taskurotta.service.hz.schedule.adapter.tasks;

import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.hz.schedule.adapter.HzJobStoreAdapter;

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
        return HzJobStoreAdapter.getRealJobStore().add(jobVO);
    }

}
