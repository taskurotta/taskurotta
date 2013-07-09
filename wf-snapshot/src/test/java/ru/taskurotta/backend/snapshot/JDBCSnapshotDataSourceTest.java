package ru.taskurotta.backend.snapshot;

import org.apache.commons.dbcp.BasicDataSource;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.snapshot.Snapshot;
import ru.taskurotta.backend.snapshot.SnapshotService;
import ru.taskurotta.backend.snapshot.SnapshotServiceImpl;
import ru.taskurotta.backend.snapshot.datasource.JDBCSnapshotDataSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;


/**
 * User: greg
 */
public class JDBCSnapshotDataSourceTest {

    private SnapshotService snapshotService;
    private Snapshot snapshot;

    @BeforeTest
    public void init() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:test_mem;");
        ds.setUsername("sa");
        ds.setPassword("sa");
        ds.setInitialSize(1);
        ds.setMaxActive(1);
        ds.setConnectionInitSqls(new ArrayList<String>() {{
            add("runscript from 'classpath:import.sql'");
        }});
        ds.setMaxIdle(1);
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(true);
        snapshotService = new SnapshotServiceImpl(new JDBCSnapshotDataSource(ds));
    }

    @Test
    public void testSave() throws Exception {
        snapshot = new Snapshot();
        Graph graph = new Graph();
        graph.setGraphId(UUID.randomUUID());
        snapshot.setGraph(graph);
        snapshot.setCreatedDate(new Date());
//        snapshotService.createSnapshot(snapshot);

    }

    @Test(dependsOnMethods = {"testSave"})
    public void testLoadSnapshotById() throws Exception {
        Snapshot founded = snapshotService.getSnapshot(snapshot.getSnapshotId());
        Assert.assertEquals(snapshot.getGraph().getGraphId(), founded.getGraph().getGraphId());
    }
}
