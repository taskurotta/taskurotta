package ru.taskurotta.transport.utils;

import junit.framework.Assert;
import org.junit.Test;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 * Created on 31.07.2015.
 */
public class TransportUtilsTest {

    @Test
    public void testGetActorDefinition() {
        String actorId = "ru.taskurotta.test.Actor#1.0";
        String taskList = "testList";
        TaskContainer tc1 = new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "test", actorId, TaskType.WORKER, -1, 3, null, null, false, null);
        ActorDefinition ad1 = TransportUtils.getActorDefinition(tc1);

        Assert.assertNotNull(ad1);
        Assert.assertEquals("ru.taskurotta.test.Actor", ad1.getName());
        Assert.assertEquals("1.0", ad1.getVersion());
        Assert.assertNull(ad1.getTaskList());

        TaskOptionsContainer toc = new TaskOptionsContainer(null, new TaskConfigContainer(null, -1, taskList, null), null);
        TaskContainer tc2 = new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "test", actorId, TaskType.WORKER, -1, 3, null, toc, false, null);
        ActorDefinition ad2 = TransportUtils.getActorDefinition(tc2);

        Assert.assertNotNull(ad2);
        Assert.assertEquals("ru.taskurotta.test.Actor", ad2.getName());
        Assert.assertEquals("1.0", ad2.getVersion());
        Assert.assertEquals(taskList, ad2.getTaskList());

        TaskContainer tc3 = null;
        ActorDefinition ad3 = TransportUtils.getActorDefinition(tc3);
        Assert.assertNull(ad3);

        TaskContainer tc4 = new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "test", "some_strange_actorId", TaskType.WORKER, -1, 3, null, toc, false, null);
        ActorDefinition ad4 = TransportUtils.getActorDefinition(tc4);
        Assert.assertNotNull(ad4);
        Assert.assertEquals("some_strange_actorId", ad4.getName());
        Assert.assertEquals("", ad4.getVersion());
        Assert.assertEquals(taskList, ad4.getTaskList());


        TaskContainer tc5 = new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "test", "some_strange_actorId#", TaskType.WORKER, -1, 3, null, toc, false, null);
        ActorDefinition ad5 = TransportUtils.getActorDefinition(tc5);
        Assert.assertNotNull(ad5);
        Assert.assertEquals("some_strange_actorId", ad5.getName());
        Assert.assertEquals("", ad5.getVersion());
        Assert.assertEquals(taskList, ad5.getTaskList());

    }

}
