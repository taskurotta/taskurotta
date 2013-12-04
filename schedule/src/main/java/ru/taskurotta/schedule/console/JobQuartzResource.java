package ru.taskurotta.schedule.console;

import com.google.common.base.Optional;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.dropwizard.resources.Action;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.adapter.HzJobMessageHandler;
import ru.taskurotta.schedule.adapter.HzMessage;
import ru.taskurotta.schedule.manager.JobManager;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Rest resource providing data on schedulers for console
 * User: dimadin
 * Date: 23.09.13 14:58
 */
@Path("/console/schedule/{action}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JobQuartzResource implements JobConstants {

    private static final Logger logger = LoggerFactory.getLogger(JobQuartzResource.class);
    private JobManager jobManager;
    private HzJobMessageHandler hzScheduleEventDispatcher;

    @GET
    public Response processGet(@PathParam("action")String action, @QueryParam("id") Optional<Long> idOpt) {
        if (Action.LIST.getValue().equals(action)) {
            Collection<Long> taskIds = jobManager.getJobIds();
            List<JobUI> result = null;
            if (taskIds!=null && !taskIds.isEmpty()) {
                result = new ArrayList<>();
                for (Long id: taskIds) {
                    JobVO jobVO = jobManager.getJob(id);
                    if (jobVO != null) {
                        JobUI jobUI = new JobUI(jobVO);
                        if (jobVO.getStatus() == STATUS_ACTIVE) {
                            jobUI.nextExecutionTime = getNextExecutionTime(jobVO.getCron());
                            jobUI.local = jobManager.isActive(jobVO);
                        }
                        result.add(jobUI);
                    }
                }
            }
            return Response.ok(result, MediaType.APPLICATION_JSON).build();

        } else if(ACTION_NODE_COUNT.equals(action)) {
            Collection<Long> jobIds = jobManager.getScheduledJobIds();
            int size = jobIds!=null? jobIds.size(): 0;
            return Response.ok(size, MediaType.APPLICATION_JSON).build();

        } else if(Action.CARD.getValue().equals(action)) {
            long id = idOpt.or(-1l);
            JobVO jobVO = jobManager.getJob(id);
            logger.debug("JobVO getted by id[{}] is [{}]", id, jobVO);
            return Response.ok(jobVO, MediaType.APPLICATION_JSON).build();

        } else {
            logger.error("Unsupported combination of method[GET] and action["+action+"].");
            return Response.serverError().build();
        }
    }

    /**
     * POJO wrapper for UI representation of JobVO with additional fields
     */
    public static class JobUI implements Serializable {
        protected Date nextExecutionTime;
        protected boolean local=false;
        protected JobVO job;

        public JobUI(JobVO job) {
            this.job = job;
        }

        public JobVO getJob() {
            return job;
        }

        public Date getNextExecutionTime() {
            return nextExecutionTime;
        }

        public boolean isLocal() {
            return local;
        }
    }

    @PUT
    public Response createOrUpdateScheduledTask(@PathParam("action")String action, @QueryParam("cron")Optional<String> cronOpt, @QueryParam("name")Optional<String> nameOpt,
                                                @QueryParam("queueLimit")Optional<Integer> queueLimitOpt, @QueryParam("jobId")Optional<Long> jobIdOpt, TaskContainer task) {
        String cron = cronOpt.or("");
        String name = nameOpt.or("");
        Integer queueLimit = queueLimitOpt.or(-1);
        Long jobId = jobIdOpt.or(-1l);

        JobVO job = new JobVO();
        job.setId(jobId);
        job.setCron(cron);
        job.setQueueLimit(queueLimit);
        job.setName(name);
        job.setTask(extendTask(task));
        job.setStatus(STATUS_INACTIVE);//modification should be applied only for inactive tasks
        validateJob(job);

        try {

            if (Action.CREATE.getValue().equals(action)) {
                logger.debug("Creating scheduled task for cron [{}], name[{}] and TaskContainer[{}]", cronOpt.or(""), nameOpt.or(""), task);
                long id = jobManager.addJob(job);
                logger.debug("Scheduled task for name[{}], cron[{}] added with id[{}]", name, cron, id);
                return Response.ok(id, MediaType.APPLICATION_JSON).build();

            } else if (Action.UPDATE.getValue().equals(action)) {
                jobManager.updateJob(job);
                logger.debug("Scheduled task with id[{}], name[{}] updated", job.getId(), job.getName());
                return Response.ok().build();

            } else {
                logger.error("Unsupported combination of method[POST] and action[" + action + "].");
                return Response.serverError().build();
            }

        } catch (Exception e) {
            logger.error("Unexpected error at action["+action+"] for job["+job+"]", e);
            throw new WebApplicationException(e);
        }

    }


    public void validateJob(JobVO job) {
        try {
            if (job == null) {
                throw new IllegalArgumentException("Job cannot be null");
            }
            CronExpression.validateExpression(job.getCron());
            if (!StringUtils.hasText(job.getName())) {
                throw new IllegalArgumentException("Job name cannot be empty");
            }
            if (job.getId()>=0 && JobConstants.STATUS_ACTIVE == jobManager.getJobStatus(job.getId())) {
                throw new IllegalArgumentException("Cannot modify active job["+job.getId()+"]!");
            }
            if (job.getTask() == null) {
                throw new IllegalArgumentException("Job cannot contain null TaskContainer!");
            } else {
                if (!StringUtils.hasText(job.getTask().getActorId())) {
                    throw new IllegalArgumentException("Job cannot contain empty Actor ID!");
                }
                if (!StringUtils.hasText(job.getTask().getMethod())) {
                    throw new IllegalArgumentException("Job cannot contain empty Method field!");
                } else if(job.getTask().getMethod().contains(" ")) {
                    throw new IllegalArgumentException("Job's method cannot contain spaces!");
                }
            }

        } catch (Throwable e) {
            logger.error("Job validation exception, job ["+job+"]", e);
            throw new WebApplicationException(e);
        }
    }

    @POST
    public Response processTask(@PathParam("action")String action, @QueryParam("id") Long id) {

        if (isActionSupported(action)) {
            logger.debug("Sending schedule message for id [{}]", id);
            hzScheduleEventDispatcher.dispatch(new HzMessage(id, action));
            return Response.ok().build();

        } else {
            logger.error("Unsupported combination of method[POST] and action[" + action + "].");
            return Response.serverError().build();
        }

    }

    private boolean isActionSupported(String action) {
        return Action.DELETE.getValue().equals(action)
                || Action.ACTIVATE.getValue().equals(action)
                || Action.DEACTIVATE.getValue().equals(action);
    }

    public static TaskContainer extendTask(TaskContainer target) {
        UUID taskId = target.getTaskId()!=null? target.getTaskId(): UUID.randomUUID();
        UUID processId = target.getProcessId()!=null? target.getProcessId(): UUID.randomUUID();
        TaskType type = target.getType()!=null? target.getType(): TaskType.DECIDER_START;
//        long startTime = target.getStartTime() != 0 ? target.getStartTime(): System.currentTimeMillis();
        long startTime = -1; // for scheduled task start time must be -1
        int numberOfAttempts = target.getNumberOfAttempts()!=0? target.getNumberOfAttempts(): 5;

        return new TaskContainer(taskId, processId, target.getMethod(), target.getActorId(), type, startTime, numberOfAttempts, target.getArgs(), target.getOptions(), target.isUnsafe());
    }


    public Date getNextExecutionTime(String cron) {
        Date result = null;

        try {
            result = new CronExpression(cron).getNextValidTimeAfter(new Date());
        } catch (ParseException e) {
            logger.error("Cannot parse cron " + cron, e);
        }

        return result;
    }

    @Required
    public void setHzScheduleEventDispatcher(HzJobMessageHandler hzScheduleEventDispatcher) {
        this.hzScheduleEventDispatcher = hzScheduleEventDispatcher;
    }

    @Required
    public void setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }
}
