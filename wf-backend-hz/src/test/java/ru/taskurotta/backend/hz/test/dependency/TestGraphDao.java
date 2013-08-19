package ru.taskurotta.backend.hz.test.dependency;

import com.hazelcast.core.Hazelcast;
import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.hz.dependency.HzGraphDao;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 16.08.13
 * Time: 14:05
 */
public class TestGraphDao {

    @Test
    public void testUpdateGraph() {
        GraphDao graphDao = new HzGraphDao(Hazelcast.newHazelcastInstance());

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
                graph.setTouchTimeMillis(touchTimeMillis);
                return true;
            }
        });

        Assert.assertTrue(result);
        Assert.assertEquals(touchTimeMillis, graphDao.getGraph(graphId).getTouchTimeMillis());
    }
}
