package ru.taskurotta.backend.snapshot;


import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;

import java.util.List;
import java.util.UUID;


/**
 * User: greg
 */
@ContextConfiguration(locations = {"classpath:application-context-test.xml"})
public class SnapshotServiceTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private SnapshotService snapshotService;

    @Mock
    private HazelcastTaskServer hazelcastTaskServer;

    @Mock
    private DependencyBackend dependencyBackend;

    private Graph graph;

    public void init() throws InterruptedException {
        MockitoAnnotations.initMocks(this);
        HazelcastTaskServer.setInstance(hazelcastTaskServer);
        graph = new Graph();
        graph.setGraphId(UUID.randomUUID());
        graph.setVersion(1);
        Mockito.when(hazelcastTaskServer.getHzInstance()).thenReturn(((SnapshotServiceImpl) snapshotService).getHazelcastInstance());
        Mockito.when(hazelcastTaskServer.getDependencyBackend()).thenReturn(dependencyBackend);
        Mockito.when(hazelcastTaskServer.getSnapshotService()).thenReturn(snapshotService);
        Mockito.when(dependencyBackend.getGraph((UUID) Matchers.anyObject())).thenReturn(graph);
    }

    @Test()
    public void testCreateSnapshot() throws Exception {
        init();
        UUID processId = UUID.randomUUID();
        snapshotService.createSnapshot(processId);
        Thread.sleep(3000);
        List<Snapshot> snapshotList = snapshotService.getSnapshotByProcessId(processId);
        Assert.assertTrue(snapshotList.get(0).getGraph().getGraphId().equals(graph.getGraphId()));
        Assert.assertEquals(graph.getVersion(), 1);
    }

}
