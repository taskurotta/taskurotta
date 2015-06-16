package ru.taskurotta.service.storage;

import org.junit.Test;
import org.testng.Assert;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * Created on 24.02.2015.
 */
public class MemoryProcessServiceTest {

    @Test
    public void testFinishedCount() {
        MemoryProcessService mps = new MemoryProcessService();
        Assert.assertEquals(0, mps.getFinishedCount(null));

        UUID procId = UUID.randomUUID();
        mps.startProcess(new TaskContainer(procId, procId, procId, "test", "testActor#1.0", TaskType.WORKER, 0l, 0,
                null, null, false, null));
        mps.finishProcess(procId, "value");

        Assert.assertEquals(1, mps.getFinishedCount(null));
    }

}
