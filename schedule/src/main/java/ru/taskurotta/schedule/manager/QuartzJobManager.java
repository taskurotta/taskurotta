package ru.taskurotta.schedule.manager;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.storage.JobStore;
import ru.taskurotta.server.TaskServer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of scheduled task manager with Quartz
 * User: dimadin
 * Date: 24.09.13 10:17
 */
public class QuartzJobManager implements JobManager {

    private static final Logger logger = LoggerFactory.getLogger(QuartzJobManager.class);

    private JobStore jobStore;
    private Scheduler scheduler;
    private QueueInfoRetriever queueInfoRetriever;
    private TaskServer taskServer;

    public QuartzJobManager() throws SchedulerException {
        this.scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
    }

    @Override
    public boolean startJob(long id) {
        boolean result = false;

        JobVO job = jobStore.getJob(id);

        if (job != null) {
            try {
                if(!isActive(job)) {
                    runJob(job);
                    jobStore.updateJobStatus(job.getId(), JobConstants.STATUS_ACTIVE);
                }
                result = true;
            } catch (Throwable e) {
                logger.error("Error starting scheduler for id["+id+"]", e);
                stopJob(id);
                jobStore.updateJobStatus(id, JobConstants.STATUS_ERROR);
            }
        }

        logger.debug("Job id[{}] start result is[{}]. JobVO is[{}]", id, result, job);

        return result;
    }

    public boolean isActive(JobVO job) {
        if (job == null) {
            throw new IllegalArgumentException("Cannot check state for null job");
        }

        boolean result = false;

        try {
            JobDetail jd = scheduler.getJobDetail(JobKey.jobKey(String.valueOf(job.getId()), job.getName()));
            result = jd!=null;
        } catch (SchedulerException e) {
            logger.error("Error determine running state for ["+job+"]", e);
        }

        logger.debug("Job name [{}] isScheduled[{}], job[{}]", job.getName(), result, job);
        return result;
    }


    public Collection<Long> getScheduledJobIds() {
        Collection<Long> result = null;

        try {
            List<String> jobGroupNames = scheduler.getJobGroupNames();
            if (jobGroupNames!=null && !jobGroupNames.isEmpty()) {
                result = new ArrayList<>();
                for (String groupName : jobGroupNames) {
                    Set<JobKey> jobKeyList = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                    if (jobKeyList!=null && !jobKeyList.isEmpty()) {
                        for (JobKey jobKey : jobKeyList) {
                            Long longValue = null;
                            String jobIdStr = jobKey.getName();
                            try {
                                longValue = Long.parseLong(jobIdStr);
                            } catch (Exception e) {
                                logger.error("Cannot parse job id ["+jobIdStr+"]", e);
                                longValue = -1l;
                            }
                            result.add(longValue);
                        }
                    }
                }
            }
        } catch(SchedulerException e) {
            String errorMessage = "Error at getting job ids: " + e.getMessage();
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }

        return result;
    }

    private void runJob(JobVO job) throws SchedulerException {
        JobDataMap jdm = new JobDataMap();
        jdm.put("job", job);
        jdm.put("queueInfoRetriever", queueInfoRetriever);
        jdm.put("taskServer", taskServer);

        JobDetail jobDetail = JobBuilder
                .newJob(EnqueueTaskJob.class)
                .withIdentity(String.valueOf(job.getId()), job.getName())
                .usingJobData(jdm)
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(String.valueOf(job.getId()), job.getName())
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(job.getCron()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        logger.debug("New job has been scheduled, [{}]", job);
    }

    private void stopJob(JobVO task) throws SchedulerException {
        boolean result = scheduler.deleteJob(JobKey.jobKey(String.valueOf(task.getId()),task.getName()));
        jobStore.updateJobStatus(task.getId(), JobConstants.STATUS_INACTIVE);
        logger.debug("Job delete result is[{}], job[{}]", result, task);
        if (!result) {
            logger.warn("Job was not found for deletion, [{}]", task);
        }
    }

    @Override
    public boolean stopJob(long id) {
        boolean result = false;
        JobVO job = jobStore.getJob(id);

        if (job != null) {
            try {
                if(isActive(job)) {
                    stopJob(job);
                    jobStore.updateJobStatus(job.getId(), JobConstants.STATUS_INACTIVE);
                }
                result = true;
            } catch (Throwable e) {
                logger.error("Error stopping scheduler for id["+id+"]", e);
            }
        }

        logger.debug("Job [{}] stop result is[{}]. JobVO is[{}]", job.getName(), result, job);

        return result;
    }

    @Override
    public int getJobStatus(long id) {
        int result = JobConstants.STATUS_UNDEFINED;
        JobVO job = jobStore.getJob(id);
        if (job!=null) {
            result = job.getStatus();
        }
        return result;
    }

    @Override
    public Date getNextExecutionTime(long id) {
        Date result = null;

        try {
            JobVO job = jobStore.getJob(id);
            if (job != null) {
                CronExpression expr = new CronExpression(job.getCron());
                result = expr.getNextValidTimeAfter(new Date());
            }
        } catch (ParseException e) {
            logger.error("Error parsing cron expression[{}] for job id [{}]", id);
        }

        return result;
    }

    @Required
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    @Required
    public void setQueueInfoRetriever(QueueInfoRetriever queueInfoRetriever) {
        this.queueInfoRetriever = queueInfoRetriever;
    }

    @Required
    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }
}
