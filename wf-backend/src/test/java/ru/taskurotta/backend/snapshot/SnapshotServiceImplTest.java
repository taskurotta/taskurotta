package ru.taskurotta.backend.snapshot;


import org.apache.commons.dbcp.BasicDataSource;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.snapshot.datasource.JDBCSnapshotDataSource;

import java.util.ArrayList;
import java.util.UUID;

/**
 * User: greg
 */
public class SnapshotServiceImplTest {


    private SnapshotService snapshotService;

    @BeforeTest
    public void init() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:test_mem1;");
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
    public void testCreateSnapshot() throws Exception {
        for (int i = 0; i < 100; i++) {
            Snapshot snap = new Snapshot(new Graph(UUID.randomUUID(), UUID.randomUUID()));
            snapshotService.createSnapshot(snap);
        }
        Thread.sleep(1000);
    }

}
