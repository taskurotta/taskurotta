package ru.taskurotta.dropwizard.resources.console.process;

import com.google.common.base.Optional;
import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Resource for retrieving tasks data
 * Date: 23.05.13 15:29
 */
@Path("/console/tasks")
public class TaskResource extends BaseResource {

    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    @GET
    public GenericPage<TaskContainer> listTasks(@QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {
        try {
            GenericPage<TaskContainer> tasksPage = consoleManager.listTasks(pageNum.or(DEFAULT_START_PAGE), pageSize.or(DEFAULT_PAGE_SIZE));
            logger.debug("Tasks page getted by pageNum[{}] and pageSize[{}] is [{}]", pageNum, pageSize, tasksPage);
            return tasksPage;

        } catch (Throwable e) {
            logger.error("Error at getting tasks list", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/task")
    public TaskContainer getTask(@QueryParam("taskId")String taskId, @QueryParam("processId")String processId) {

        try {
            TaskContainer taskContainer = consoleManager.getTask(UUID.fromString(taskId), UUID.fromString(processId));
            logger.debug("Task got by id[{}] and processId[{}] is [{}]", taskId, processId, taskContainer);
            return taskContainer;

        } catch(Throwable e) {
            logger.error("Error at getting task by id["+taskId+"], processId["+processId+"]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/decision/{processId}/{taskId}")
    public DecisionContainer getTaskDecision(@PathParam("processId")String processId, @PathParam("taskId")String taskId) {

        try {
            DecisionContainer result = consoleManager.getDecision(UUID.fromString(taskId), UUID.fromString(processId));
            logger.debug("DecisionContainer getted by taskId[{}], processId[{}] is [{}]", taskId, processId, result);
            return result;

        } catch(Throwable e) {
            logger.error("Error at getting task decision by taskId["+taskId+"], processId["+processId+"]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/search")
    public List<TaskContainer> findTasks(@QueryParam("taskId") Optional<String> taskId, @QueryParam("processId") Optional<String> processId) {

        try {
            List<TaskContainer> result = consoleManager.findTasks(processId.or(""), taskId.or(""));
            logger.debug("Task found by id[{}], processId[{}] is  [{}]", taskId, processId, result);
            return result;

        } catch (Throwable e) {
            logger.error("Error at getting task by id["+taskId+"], processId["+processId+"]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/process/{processId}")
    public Collection<TaskContainer> getProcessTasks(@PathParam("processId") String processId) {

        try {
            Collection<TaskContainer> tasks = consoleManager.getProcessTasks(UUID.fromString(processId));
            logger.debug("Task list getted by processId[{}] is [{}]", processId, tasks);
            return tasks;

        } catch (Throwable e) {
            logger.error("Error at getting process tasks by processId[" + processId + "]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }


}
