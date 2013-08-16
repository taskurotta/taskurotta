package ru.taskurotta.backend.test.dependency.links;

import org.junit.Test;
import org.testng.Assert;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.dependency.links.MemoryGraphDao;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 16.08.13
 * Time: 14:05
 */
public class TestGraphDao {

    @Test
    public void testUpdateGraph() {
        GraphDao graphDao = new MemoryGraphDao();

        final UUID graphId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        graphDao.createGraph(graphId, taskId);

        final long touchTimeMillis = System.currentTimeMillis();

        boolean result = graphDao.changeGraph(new GraphDao.Updater() {
            @Override
            public UUID getProcessId() {
                return graphId;
            }

            @Override
            public boolean apply(Graph graph) {
                graph.setVersion(graph.getVersion() + 1);
                graph.setTouchTimeMillis(touchTimeMillis);
                return true;
            }
        });

        Assert.assertTrue(result);
        Assert.assertEquals(touchTimeMillis, graphDao.getGraph(graphId).getTouchTimeMillis());
    }
}
