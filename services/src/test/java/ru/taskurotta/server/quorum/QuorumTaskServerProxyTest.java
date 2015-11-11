package ru.taskurotta.server.quorum;

import org.junit.Test;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import static org.junit.Assert.assertNotNull;

/**
 */
public class QuorumTaskServerProxyTest {

    private QuorumTaskServerProxy quorumTaskServerProxy;
    private MockClusterQuorum mockClusterQuorum;

    private class MockClusterQuorum implements ClusterQuorum {
        boolean isPresent;

        @Override
        public boolean isPresent() {
            return isPresent;
        }

        public void setPresent(boolean isPresent) {
            this.isPresent = isPresent;
        }
    }

    public QuorumTaskServerProxyTest() {
        mockClusterQuorum = new MockClusterQuorum();

        quorumTaskServerProxy = new QuorumTaskServerProxy(new TaskServer() {
            @Override
            public void startProcess(TaskContainer task) {

            }

            @Override
            public TaskContainer poll(ActorDefinition actorDefinition) {
                return null;
            }

            @Override
            public void release(DecisionContainer taskResult) {

            }
        }, mockClusterQuorum);
    }

    @Test
    public void testStartProcess() throws Exception {
        setQuorumPresent();
        quorumTaskServerProxy.startProcess(null);
        setQuorumAbsent();

        OutOfQuorumException ex = null;

        try {
            quorumTaskServerProxy.startProcess(null);
        } catch (OutOfQuorumException e) {
            ex = e;
        }

        assertNotNull(ex);

    }

    private void setQuorumAbsent() {
        mockClusterQuorum.setPresent(false);
    }

    private void setQuorumPresent() {
        mockClusterQuorum.setPresent(true);
    }

    @Test
    public void testPoll() throws Exception {
        setQuorumPresent();
        quorumTaskServerProxy.poll(null);
        setQuorumAbsent();

        OutOfQuorumException ex = null;

        try {
            quorumTaskServerProxy.poll(null);
        } catch (OutOfQuorumException e) {
            ex = e;
        }

        assertNotNull(ex);
    }

    @Test
    public void testRelease() throws Exception {
        setQuorumPresent();
        quorumTaskServerProxy.release(null);
        setQuorumAbsent();

        OutOfQuorumException ex = null;

        try {
            quorumTaskServerProxy.release(null);
        } catch (OutOfQuorumException e) {
            ex = e;
        }

        assertNotNull(ex);
    }
}