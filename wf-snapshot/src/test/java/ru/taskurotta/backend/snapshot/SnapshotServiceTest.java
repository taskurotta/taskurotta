package ru.taskurotta.backend.snapshot;



import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;

import java.util.UUID;

import static org.powermock.api.mockito.PowerMockito.when;

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
        when(HazelcastTaskServer.getInstance()).thenReturn(hazelcastTaskServer);
//        Assert.assertEquals(HazelcastTaskServer.getInstance(), hazelcastTaskServer);
    }

    @Test(enabled = false)
    public void testCreateSnapshot() throws Exception {
        snapshotService.createSnapshot(UUID.randomUUID());
        Thread.sleep(5000);
    }


    @ObjectFactory
    public org.testng.IObjectFactory setObjectFactory() {
        return new PowerMockObjectFactory();
    }

}
