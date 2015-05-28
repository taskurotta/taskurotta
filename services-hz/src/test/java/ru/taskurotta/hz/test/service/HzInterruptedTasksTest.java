package ru.taskurotta.hz.test.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.InterruptedTaskExt;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.hz.storage.HzInterruptedTasksService;

import java.util.Collection;
import java.util.UUID;

/**
 * Created on 24.02.2015.
 */
public class HzInterruptedTasksTest {

    private static final Logger logger = LoggerFactory.getLogger(HzInterruptedTasksTest.class);

    private String targetMapName = "testBpProc";

    private long MS_IN_HOUR = 60*60*1000;

    HzInterruptedTasksService target;
    HazelcastInstance hzInstance;

    @Before
    public void setUp() {
        TestHazelcastInstanceFactory factory = new TestHazelcastInstanceFactory(1);
        hzInstance = factory.newHazelcastInstance();
        target = new HzInterruptedTasksService(hzInstance, targetMapName);
    }

    @Test
    public void testSaveAndFind() {
        InterruptedTaskExt p1 = getNewTask();
        target.save(p1, p1.getFullMessage(), p1.getStackTrace());

        InterruptedTaskExt p2 = getNewTask();
        target.save(p2, p2.getFullMessage(), p2.getStackTrace());

        IMap<UUID, InterruptedTask> hzMap = hzInstance.getMap(targetMapName);
        for (InterruptedTask it : hzMap.values()) {
            logger.debug("Stored interrupted task hzMap value[{}]", it);
        }
        Assert.assertEquals(2, hzMap.size());

        Collection<InterruptedTask> findRes = null;

        SearchCommand c1 = new SearchCommand();
        c1.setStarterId(toTwoThirdsLength(p1.getStarterId()));
        findRes = target.find(c1);
        Assert.assertEquals(2, findRes.size());

        SearchCommand c2 = new SearchCommand();
        c2.setActorId(toTwoThirdsLength(p1.getActorId()));
        findRes = target.find(c2);
        Assert.assertEquals(2, findRes.size());

        SearchCommand c3 = new SearchCommand();
        c3.setErrorClassName(toTwoThirdsLength(p1.getErrorClassName()));
        findRes = target.find(c3);
        Assert.assertEquals(2, findRes.size());

        SearchCommand c4 = new SearchCommand();
        c4.setErrorMessage(toTwoThirdsLength(p1.getErrorMessage()));
        findRes = target.find(c4);
        Assert.assertEquals(2, findRes.size());

        SearchCommand c5 = new SearchCommand();
        c5.setStartPeriod(System.currentTimeMillis() - MS_IN_HOUR);
        findRes = target.find(c5);
        Assert.assertEquals(2, findRes.size());

        SearchCommand c6 = new SearchCommand();
        c6.setEndPeriod(System.currentTimeMillis() + MS_IN_HOUR);
        findRes = target.find(c6);
        Assert.assertEquals(2, findRes.size());

        SearchCommand cId1 = new SearchCommand();
        cId1.setTaskId(p1.getTaskId());
        findRes = target.find(cId1);
        Assert.assertEquals(1, findRes.size());

        SearchCommand cId2 = new SearchCommand();
        cId2.setTaskId(p2.getTaskId());
        findRes = target.find(cId2);
        Assert.assertEquals(1, findRes.size());

        SearchCommand cId3 = new SearchCommand();
        cId3.setTaskId(UUID.randomUUID());
        findRes = target.find(cId3);
        Assert.assertNull(findRes);

        InterruptedTaskExt p3 = getNewTask();
        p3.setErrorClassName(IllegalArgumentException.class.getName());
        SearchCommand com = new SearchCommand();
        com.setStarterId(p3.getStarterId());
        com.setErrorClassName(p3.getErrorClassName());
        target.save(p3, p3.getFullMessage(), p3.getStackTrace());
        findRes = target.find(com);
        Assert.assertEquals(1, findRes.size());

    }

    @Test
    public void testAddAndDelete() {
        InterruptedTaskExt task1 = getNewTask();
        InterruptedTaskExt task2 = getNewTask();

        target.save(task1, task1.getFullMessage(), task1.getStackTrace());
        target.save(task2, task2.getFullMessage(), task2.getStackTrace());

        Assert.assertEquals(2, target.findAll().size());

        target.delete(task1.getProcessId(), task1.getTaskId());
        Assert.assertEquals(1, target.findAll().size());

        target.delete(task2.getProcessId(), task2.getTaskId());
        Collection<InterruptedTask> colection = target.findAll();
        Assert.assertEquals(0, colection!=null?colection.size(): 0);
    }

    private String toTwoThirdsLength(String target) {
        return target.substring(0, target.length()*2/3);
    }

    private InterruptedTaskExt getNewTask() {
        InterruptedTaskExt proc = new InterruptedTaskExt();
        proc.setStarterId("ru.tsk.test.Starter#1.1");
        proc.setActorId("ru.tsk.test.Actor#2.6");
        proc.setErrorClassName(UnsupportedOperationException.class.getName());
        proc.setErrorMessage("just testing");
        proc.setProcessId(UUID.randomUUID());
        proc.setTaskId(UUID.randomUUID());
        proc.setTime(System.currentTimeMillis());

        proc.setStackTrace("some traces");
        proc.setFullMessage("just testing 2");

        return proc;
    }

}
