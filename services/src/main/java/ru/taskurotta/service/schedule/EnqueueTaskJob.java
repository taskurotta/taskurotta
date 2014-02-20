package ru.taskurotta.service.schedule;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: dimadin
 * Date: 24.09.13 12:40
 */
public class EnqueueTaskJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EnqueueTaskJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jdm = context.getJobDetail().getJobDataMap();
        JobVO job = (JobVO) jdm.get(JobConstants.DATA_KEY_JOB);
        JobManager jobManager = (JobManager) jdm.get(JobConstants.DATA_KEY_JOB_MANAGER);
        TaskContainer taskContainer = job != null ? job.getTask() : null;
        TaskServer taskServer = (TaskServer) jdm.get(JobConstants.DATA_KEY_TASK_SERVER);
        QueueInfoRetriever queueInfoRetriever = (QueueInfoRetriever) jdm.get(JobConstants.DATA_KEY_QUEUE_INFO_RETRIEVER);

        try {

            validateEntities(taskServer, job, queueInfoRetriever, jobManager);

            if (job.getQueueLimit() > 0) {
                int size = queueInfoRetriever.getQueueTaskCount(taskContainer.getActorId());
                if (size >= job.getQueueLimit()) {
                    logger.debug("Queue [{}] contains [{}] elements. Skip task due to limit[{}].", taskContainer.getActorId(), size, job.getQueueLimit());
                    return;
                }
            }

            taskContainer = renewTaskGuids(taskContainer);
            logger.debug("Starting process for task [{}] on schedule", taskContainer);

            taskServer.startProcess(taskContainer);

            if (job.getErrorCount() > 0) {
                job.setErrorCount(0);
                job.setLastError("");
                jobManager.updateErrorCount(job.getId(), job.getErrorCount(), job.getLastError());//reset error counter
            }

        } catch (Throwable e) {
            logger.error("Cannot execute scheduled job for task [" + taskContainer + "]", e);

            if (jobManager != null && job != null && job.getId() > 0) {
                job.setErrorCount(job.getErrorCount() + 1);
                job.setLastError(e.getClass().getName() + ": " + e.getMessage());
                try {
                    jobManager.updateErrorCount(job.getId(), job.getErrorCount(), job.getLastError());
                    int maxErrCount = job.getMaxErrors();
                    if (maxErrCount>0 && job.getErrorCount() + 1 >= maxErrCount) {
                        if (jobManager.stopJob(job.getId())) {
                            jobManager.updateJobStatus(job.getId(), JobConstants.STATUS_ERROR);
                        }
                    }

                } catch (Throwable err) {
                    logger.error("Error at error handling for job [" + job + "]", e);
                }

            }
        }
    }


    public void validateEntities(TaskServer taskServer, JobVO job, QueueInfoRetriever queueInfoRetriever, JobManager jobManager) {
        if (taskServer == null) {
            throw new IllegalArgumentException("Scheduled job have no TaskServer data entity!");
        }
        if (job == null) {
            throw new IllegalArgumentException("Scheduled job have no JobVO data entity!");
        }
        if (jobManager == null) {
            throw new IllegalArgumentException("Scheduled job have no JobManager data entity!");
        }
        if (job.getTask() == null) {
            throw new IllegalArgumentException("Scheduled job have no TaskContainer data entity!");
        }
        if (job.getQueueLimit() > 0 && queueInfoRetriever == null) {
            throw new IllegalArgumentException("Scheduled job have no QueueInfoRetriever data entity!");
        }
    }

    public static TaskContainer renewTaskGuids(TaskContainer target) {
        UUID newGuid = UUID.randomUUID();
        return new TaskContainer(newGuid, newGuid, target.getMethod(), target.getActorId(), target.getType(), target.getStartTime(), target.getNumberOfAttempts(), target.getArgs(), target.getOptions(), target.isUnsafe(), target.getFailTypes());
    }


}
