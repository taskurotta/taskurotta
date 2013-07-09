package ru.taskurotta.backend.snapshot;

import com.hazelcast.core.PartitionAware;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * User: greg
 */
public class TaskToSave implements Callable, PartitionAware, Serializable {

    private UUID processId;

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public TaskToSave() {

    }

    public TaskToSave(UUID processId) {
        this.processId = processId;
    }

    @Override
    public Object call() throws Exception {
//        HazelcastTaskServer taskServer = HazelcastTaskServer.getInstance();
        System.out.println(processId);
        return null;
    }

    @Override

    public Object getPartitionKey() {
        return processId;
    }
}