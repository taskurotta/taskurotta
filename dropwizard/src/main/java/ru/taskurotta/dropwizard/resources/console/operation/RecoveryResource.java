package ru.taskurotta.dropwizard.resources.console.operation;

import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryThreads;
import ru.taskurotta.service.recovery.RecoveryOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * Resource providing info on current recovery workload
 * Date: 14.01.14 10:58
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/operation/recovery")
public class RecoveryResource {

    private OperationExecutor recoveryOperationExecutor;

    private RecoveryThreads recoveryThreads;

    @GET
    @Path("/size")
    public Integer getOperationExecutorSize() {
        return recoveryOperationExecutor.size();
    }

    @POST
    @Path("/add")
    public void addProcessToRecovery(String processId) {
        recoveryOperationExecutor.enqueue(new RecoveryOperation(UUID.fromString(processId)));
    }

    @POST
    @Path("/finder/state")
    public void setProcessFinderStarted(Boolean started) {
        if (started) {
            recoveryThreads.start();
        } else {
            recoveryThreads.stop();
        }
    }

    @GET
    @Path("/finder/state")
    public Boolean isProcessFinderStarted() {
        return recoveryThreads.isStarted();
    }

    @Required
    public void setRecoveryOperationExecutor(OperationExecutor recoveryOperationExecutor) {
        this.recoveryOperationExecutor = recoveryOperationExecutor;
    }

    @Required
    public void setRecoveryThreads(RecoveryThreads recoveryThreads) {
        this.recoveryThreads = recoveryThreads;
    }
}
