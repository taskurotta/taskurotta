package ru.taskurotta.service.schedule.storage;

import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.storage.EntityStore;

/**
 * User: dimadin
 * Date: 23.09.13 10:31
 */
public interface JobStore extends EntityStore<JobVO> {

    void updateJobStatus(long id, int status);

    int getJobStatus(long jobId);

    void updateErrorCount(long jobId, int count, String message);

}
