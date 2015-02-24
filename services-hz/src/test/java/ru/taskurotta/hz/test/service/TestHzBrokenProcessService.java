package ru.taskurotta.hz.test.service;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.BrokenProcess;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.hz.storage.HzBrokenProcessService;

import java.util.Collection;
import java.util.UUID;

/**
 * Created on 24.02.2015.
 */
public class TestHzBrokenProcessService {

    private static final Logger logger = LoggerFactory.getLogger(TestHzBrokenProcessService.class);

    private String targetMapName = "testBpProc";

    private long MS_IN_HOUR = 60*60*1000;

    HzBrokenProcessService target;
    HazelcastInstance hzInstance;

    @Before
    public void setUp() {
        hzInstance = Hazelcast.newHazelcastInstance();
        target = new HzBrokenProcessService(hzInstance, targetMapName);
    }

    @Test
    public void testSaveAndFind() {
        BrokenProcess p1 = getNewProcess();
        target.save(p1);

        BrokenProcess p2 = getNewProcess();
        target.save(p2);

        IMap<UUID, BrokenProcess> hzMap = hzInstance.getMap(targetMapName);
        for (BrokenProcess bp : hzMap.values()) {
            logger.debug("Stored broken process hzMap value[{}]", bp);
        }
        Assert.assertEquals(2, hzMap.size());

        Collection<BrokenProcess> findRes = null;

        SearchCommand c1 = new SearchCommand();
        c1.setStartActorId(toTwoThirdsLength(p1.getStartActorId()));
        findRes = target.find(c1);
        Assert.assertEquals(2, findRes.size());

        SearchCommand c2 = new SearchCommand();
        c2.setBrokenActorId(toTwoThirdsLength(p1.getBrokenActorId()));
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
        cId1.setProcessId(p1.getProcessId());
        findRes = target.find(cId1);
        Assert.assertEquals(1, findRes.size());

        SearchCommand cId2 = new SearchCommand();
        cId2.setProcessId(p2.getProcessId());
        findRes = target.find(cId2);
        Assert.assertEquals(1, findRes.size());

        SearchCommand cId3 = new SearchCommand();
        cId3.setProcessId(UUID.randomUUID());
        findRes = target.find(cId3);
        Assert.assertNull(findRes);

        BrokenProcess p3 = getNewProcess();
        p3.setErrorClassName(IllegalArgumentException.class.getName());
        SearchCommand com = new SearchCommand();
        com.setStartActorId(p3.getStartActorId());
        com.setErrorClassName(p3.getErrorClassName());
        target.save(p3);
        findRes = target.find(com);
        Assert.assertEquals(1, findRes.size());

    }

    private String toTwoThirdsLength(String target) {
        return target.substring(0, target.length()*2/3);
    }

    private BrokenProcess getNewProcess() {
        BrokenProcess proc = new BrokenProcess();
        proc.setStartActorId("ru.tsk.test.Starter#1.1");
        proc.setBrokenActorId("ru.tsk.test.Actor#2.6");
        proc.setErrorClassName(UnsupportedOperationException.class.getName());
        proc.setErrorMessage("just testing");
        proc.setProcessId(UUID.randomUUID());
        proc.setStackTrace("some traces");
        proc.setTime(System.currentTimeMillis());
        return proc;
    }

}
