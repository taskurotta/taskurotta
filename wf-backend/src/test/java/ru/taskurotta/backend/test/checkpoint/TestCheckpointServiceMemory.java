package ru.taskurotta.backend.test.checkpoint;

import junit.framework.Assert;
import org.junit.Test;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import java.util.List;
import java.util.UUID;

public class TestCheckpointServiceMemory {

    @Test
    public void testCheckpointServiceMemory() {
        CheckpointService checkpointService = new MemoryCheckpointService();

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        String type1 = "type1";
        String type2 = "type2";

        long time1 = System.currentTimeMillis();
        long time2 = System.currentTimeMillis() + 100l;
        long time3 = System.currentTimeMillis() + 1000l;

        Checkpoint c1 = new Checkpoint(TimeoutType.TASK_START_TO_CLOSE, uuid1, type1, time1);
        Checkpoint c2 = new Checkpoint(TimeoutType.TASK_START_TO_CLOSE, uuid2, type1, time2);
        Checkpoint c3 = new Checkpoint(TimeoutType.TASK_START_TO_CLOSE, uuid3, type2, time3);

        checkpointService.addCheckpoint(c1);
        checkpointService.addCheckpoint(c2);
        checkpointService.addCheckpoint(c3);

        CheckpointQuery query1 = new CheckpointQuery(TimeoutType.TASK_START_TO_CLOSE);
        query1.setMaxTime(time3 + 100l);
        query1.setEntityType(type1);
        query1.setMinTime(time1 - 100);

        CheckpointQuery query2 = new CheckpointQuery(TimeoutType.TASK_START_TO_CLOSE);
        query2.setMaxTime(time3 + 100l);
        query2.setEntityType(type2);
        query2.setMinTime(time1 - 100);


        List<Checkpoint> result1 = checkpointService.listCheckpoints(query1);
        Assert.assertNotNull(result1);
        Assert.assertEquals("Must find 2 checkpoints", 2, result1.size());

        List<Checkpoint> result2 = checkpointService.listCheckpoints(query2);
        Assert.assertNotNull(result2);
        Assert.assertEquals("Must find 1 checkpoints", 1, result2.size());

        checkpointService.removeCheckpoint(new Checkpoint(TimeoutType.TASK_START_TO_CLOSE, c1.getEntityGuid(), c1.getEntityType(), c1.getTime()));
        List<Checkpoint> result3 = checkpointService.listCheckpoints(query1);
        Assert.assertNotNull(result3);
        Assert.assertEquals("Must find 1 checkpoints", 1, result3.size());

    }


}
