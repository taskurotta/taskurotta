package ru.taskurotta.backend.snapshot.datasource;

import org.apache.commons.dbcp.BasicDataSource;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.snapshot.Snapshot;
import ru.taskurotta.backend.snapshot.SnapshotService;
import ru.taskurotta.backend.snapshot.SnapshotServiceImpl;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskType;

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
        ds.setConnectionInitSqls(new ArrayList<String>() {{add("runscript from 'classpath:import.sql'");}});
        ds.setMaxIdle(1);
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(true);
        snapshotService = new SnapshotServiceImpl(new JDBCSnapshotDataSource(ds));
        snapshot = new Snapshot(
                new TaskImpl(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new TaskTargetImpl(TaskType.DECIDER_START, "", "", ""),
                        0,
                        1,
                        new Object[]{},
                        new TaskOptionsImpl(null)),
                new Graph(UUID.randomUUID(), UUID.randomUUID()),
                new DependencyDecision());
    }

    @Test
    public void testSave() throws Exception {
        snapshot = new Snapshot(
                new TaskImpl(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new TaskTargetImpl(TaskType.DECIDER_START, "", "", ""),
                        0,
                        1,
                        new Object[]{},
                        new TaskOptionsImpl(null)),
                new Graph(UUID.randomUUID(), UUID.randomUUID()),
                new DependencyDecision());
        snapshot.setCreatedDate(new Date());
        snapshotService.createSnapshot(snapshot);

    }

    @Test(dependsOnMethods = {"testSave"})
    public void testLoadSnapshotById() throws Exception {
        Snapshot founded = snapshotService.getSnapshot(snapshot.getSnapshotId());
        Assert.assertEquals(founded.getTask().getId(), snapshot.getTask().getId());
    }
}
