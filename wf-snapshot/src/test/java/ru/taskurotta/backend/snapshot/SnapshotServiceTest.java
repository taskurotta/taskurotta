package ru.taskurotta.backend.snapshot;


import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.dependency.links.Graph;
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

    @Mock
    DependencyBackend dependencyBackend;

    public void init() throws InterruptedException {
        MockitoAnnotations.initMocks(this);
        HazelcastTaskServer.setInstance(hazelcastTaskServer);
        Graph graph = new Graph();
        graph.setGraphId(UUID.randomUUID());
        graph.setVersion(1);
        Mockito.when(hazelcastTaskServer.getHzInstance()).thenReturn(((SnapshotServiceImpl) snapshotService).getHazelcastInstance());
        Mockito.when(hazelcastTaskServer.getDependencyBackend()).thenReturn(dependencyBackend);
        Mockito.when(dependencyBackend.getGraph((UUID) Matchers.anyObject())).thenReturn(graph);
    }

    @Test()
    public void testCreateSnapshot() throws Exception {
        init();
        snapshotService.createSnapshot(UUID.randomUUID());
        Thread.sleep(3000);
    }

}
