package ru.taskurotta.dropwizard.resources.console;

import com.google.common.base.Optional;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 31.05.13 12:16
 */
@Path("/console/tasks")
public class TaskListResource extends BaseResource {
    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    @GET
    public Response listTasks(@QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize) {
        try {
            int pgSize = pageSize.or(-1);
            GenericPage<TaskContainer> tasksPage = consoleManager.listTasks(pageNum.or(DEFAULT_START_PAGE), pageSize.or(DEFAULT_PAGE_SIZE));
            logger.debug("Tasks page getted by pageNum[{}] and pageSize[{}] is [{}]", pageNum, pageSize, tasksPage);
            return Response.ok(tasksPage, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            logger.error("Error at getting tasks list", e);
            return Response.serverError().build();
        }
    }

}
