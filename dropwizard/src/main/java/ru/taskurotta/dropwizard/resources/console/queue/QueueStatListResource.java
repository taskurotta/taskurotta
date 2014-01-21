package ru.taskurotta.dropwizard.resources.console.queue;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.dropwizard.resources.console.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

/**
 * Resource for obtaining queue stat list info
 * Date: 21.05.13 11:49
 */
@Path("/console/queues")
public class QueueStatListResource extends BaseResource {
    private static final Logger logger = LoggerFactory.getLogger(QueueStatListResource.class);

    private QueueInfoRetriever queueInfoRetriever;

    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;

    @GET
    public GenericPage<QueueStatVO> getQueuesPage(@QueryParam("pageNum") Optional<Integer> pageNum, @QueryParam("pageSize") Optional<Integer> pageSize, @QueryParam("filter") Optional<String> filter) {
        try {
            GenericPage<QueueStatVO> queuesStatInfo = consoleManager.getQueuesStatInfo(pageNum.or(DEFAULT_START_PAGE), pageSize.or(DEFAULT_PAGE_SIZE), filter.or(""));
            if (queuesStatInfo!=null && queuesStatInfo.getItems()!=null && !queuesStatInfo.getItems().isEmpty()) {
                for (QueueStatVO qs : queuesStatInfo.getItems()) {
                    long time = queueInfoRetriever.getLastPolledTaskEnqueueTime(qs.getName());
                    logger.debug("LastPolledTaskEnqueueTime for queue [{}] is [{}]", qs.getName(), time);
                    qs.setLastPolledTaskEnqueueTime(time);
                }
            }
            logger.debug("QueueStatVO page is [{}]", queuesStatInfo);
            return queuesStatInfo;
        } catch (Throwable e) {
            logger.error("Error at getting queues stat list", e);
            throw new WebApplicationException(e);
        }
    }

    @Path("/{queueName}/size")
    @GET
    public Integer getQueueRealSize(@PathParam("queueName") String queueName) {
        try {
            int queueSize = queueInfoRetriever.getQueueTaskCount(queueName);
            logger.debug("Queue [{}] real size is [{}]", queueName, queueSize);
            return queueSize;
        } catch (Throwable e) {
            logger.error("Error at getting queue["+queueName+"] real size", e);
            throw new WebApplicationException(e);
        }
    }

//    @Path("/{queueName}/stat_time")
//    @GET
//    public Long getLastPolledTaskEnqueueTime(@PathParam("queueName") String queueName) {
//        try {
//            long lastPolledTaskEnqueueTime = queueInfoRetriever.getLastPolledTaskEnqueueTime(queueName);
//            logger.debug("Queue [{}] lastPolledTaskEnqueueTime is [{}]", queueName, lastPolledTaskEnqueueTime);
//            return lastPolledTaskEnqueueTime;
//        } catch (Throwable e) {
//            logger.error("Error at getting queue["+queueName+"] lastPolledTaskEnqueueTime", e);
//            throw new WebApplicationException(e);
//        }
//    }

    @Required
    public void setQueueInfoRetriever(QueueInfoRetriever queueInfoRetriever) {
        this.queueInfoRetriever = queueInfoRetriever;
    }
}
