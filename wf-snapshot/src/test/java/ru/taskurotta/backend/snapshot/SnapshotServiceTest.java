package ru.taskurotta.backend.snapshot;


import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;

import java.util.UUID;


/**
 * User: greg
 */
@ContextConfiguration(locations = {"classpath:application-context-test.xml"})
public class SnapshotServiceTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private SnapshotService snapshotService;

    @Mock
    HazelcastTaskServer hazelcastTaskServer;

    @BeforeTest
    public void init() throws InterruptedException {
        MockitoAnnotations.initMocks(this);
        HazelcastTaskServer.setInstance(hazelcastTaskServer);
    }

    @Test()
    public void testCreateSnapshot() throws Exception {
        HazelcastTaskServer hts = HazelcastTaskServer.getInstance();
        Assert.assertEquals(hts, hazelcastTaskServer);
        snapshotService.createSnapshot(UUID.randomUUID());
    }

}
