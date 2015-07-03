package ru.taskurotta.dropwizard.resources.console.schedule;

import com.google.common.base.Optional;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.dropwizard.resources.console.schedule.model.CreateJobCommand;
import ru.taskurotta.dropwizard.resources.console.util.TaskContainerUtils;
import ru.taskurotta.service.console.Action;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.PageCommand;
import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.service.schedule.JobManager;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.transport.utils.TransportUtils;

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
import javax.ws.rs.core.Response.Status;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Rest resource providing data on schedulers for console
 * User: dimadin
 * Date: 23.09.13 14:58
 */
@Path("/console/schedule")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SchedulerResource implements JobConstants {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerResource.class);
    private JobManager jobManager;

    @GET
    @Path("/list")
    public GenericPage<JobUI> listScheduleJobs(@QueryParam("pageNum") Optional<Integer> pageNumOpt,
                                        @QueryParam("pageSize") Optional<Integer> pageSizeOpt) {
        List<JobUI> items = null;
        int pageNum = pageNumOpt.or(PageCommand.DEFAULT_PAGE_NUM);
        int pageSize = pageSizeOpt.or(PageCommand.DEFAULT_PAGE_SIZE);

        Collection<Long> taskIds = jobManager.getJobIds();
        int total = taskIds!=null? taskIds.size() : 0;
        if (taskIds!=null && !taskIds.isEmpty()) {
            List<Long> keysSorted = new ArrayList<>(taskIds);
            Collections.sort(keysSorted);
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = Math.min(pageSize * pageNum, total);
            items = new ArrayList<>(pageEnd-pageStart);
            logger.debug("Try to paginate keys list[{}] with pageStart[{}], pageEnd[{}]", keysSorted, pageStart, pageEnd);
            for (int i = pageStart; i < pageEnd; i++) {
                JobVO jobVO = jobManager.getJob(keysSorted.get(i));
                if (jobVO != null) {
                    JobUI jobUI = new JobUI(jobVO);
                    if (jobVO.getStatus() == STATUS_ACTIVE) {
                        jobUI.nextExecutionTime = jobManager.getNextExecutionTime(jobVO.getCron());
                        jobUI.local = jobManager.isActive(jobVO);
                    }
                    items.add(jobUI);
                }
            }
        }
        logger.debug("Items are [{}], pageNum[{}], pageSize[{}], total[{}]", items, pageNum, pageSize, total);
        return new GenericPage<>(items, pageNum, pageSize, total);
    }

    @GET
    @Path("/node_count")
    public Integer getNodesCount(){
        Collection<Long> jobIds = jobManager.getScheduledJobIds();
        int size = jobIds!=null? jobIds.size(): 0;
        return size;
    }

    @GET
    @Path("/card")
    public CreateJobCommand getJob(@QueryParam("id") Optional<Long> idOpt) {
        long id = idOpt.or(-1l);
        CreateJobCommand result = asCommand(jobManager.getJob(id));
        logger.debug("JobVO got by id[{}] is [{}]", id, result);
        return result;
    }

    private CreateJobCommand asCommand(JobVO jobVO) {
        CreateJobCommand result = null;
        if (jobVO != null) {
            result = new CreateJobCommand();
            result.setId(jobVO.getId());
            result.setCron(jobVO.getCron());
            result.setName(jobVO.getName());
            result.setMaxErrors(jobVO.getMaxErrors());
            result.setQueueLimit(jobVO.getQueueLimit());

            if (jobVO.getTask() != null) {
                result.setStatus(jobVO.getStatus());
                result.setActorId(jobVO.getTask().getActorId());
                result.setArgs(TaskContainerUtils.asUiArguments(jobVO.getTask().getArgs()));
                result.setMethod(jobVO.getTask().getMethod());
                result.setTaskType(jobVO.getTask().getType());
                result.setTaskList(TransportUtils.getTaskList(jobVO.getTask()));
            }
        }
        return result;
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
    @Path("/create")
    public Long createSchedulerJob(CreateJobCommand command) {
        JobVO job = null;
        try {
            job = getValidJob(command);
            logger.debug("Creating scheduled task for command[{}]", command);
            long id = jobManager.addJob(job);
            logger.debug("Scheduled task for command[{}] added with id[{}]", command, id);
            return id;

        } catch (Exception e) {
            logger.error("Unexpected error at create for command["+command+"]", e);
            throw new WebApplicationException(e);
        }

    }

    @PUT
    @Path("/update")
    public void updateSchedulerJob(CreateJobCommand command) {
        JobVO job = null;
        try {
            job = getValidJob(command);
            jobManager.updateJob(job);
            logger.debug("Scheduled task with id[{}], name[{}] updated", job.getId(), job.getName());

        } catch (Exception e) {
            logger.error("Unexpected error at update for command["+command+"]", e);
            throw new WebApplicationException(e);
        }

    }

    protected JobVO getValidJob (CreateJobCommand command) {

        JobVO job = new JobVO();
        job.setId(command.getId());
        job.setCron(command.getCron());
        job.setQueueLimit(command.getQueueLimit());
        job.setMaxErrors(command.getMaxErrors());
        job.setName(command.getName());
        job.setTask(TaskContainerUtils.createTask(command, -1));// for scheduled task start time must be -1
        job.setStatus(STATUS_INACTIVE);//modification should be applied only for inactive tasks
        validateJob(job);

        return job;
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
    @Path("/action/{action}")
    public void processTask(@PathParam("action")String action, Long id) {
        logger.debug("Sending schedule message for id [{}]", id);

        if (id!=null && Action.ACTIVATE.getValue().equals(action)) {
            jobManager.startJob(id);

        } else if (id!=null && Action.DEACTIVATE.getValue().equals(action)) {
            jobManager.stopJob(id);

        } else if (id!=null && Action.DELETE.getValue().equals(action)) {
            jobManager.removeJob(id);

        } else {
            logger.error("Unsupported combination of method[POST] and action[" + action + "] for id["+id+"].");
            throw new WebApplicationException(Status.NOT_ACCEPTABLE);
        }
    }

    @Required
    public void setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }
}
