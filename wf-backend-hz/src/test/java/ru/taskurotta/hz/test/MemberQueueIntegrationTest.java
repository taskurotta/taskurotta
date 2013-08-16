package ru.taskurotta.hz.test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;
import org.junit.Test;
import ru.taskurotta.backend.hz.util.MemberQueue;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 8/15/13
 * Time: 2:13 PM
 */
public class MemberQueueIntegrationTest {

    private static final String QUEUE_NAME = "test";

    class TestServer {

        private int number;
        private IQueue<Long> queue;

        private HazelcastInstance hzInstance;
        private PartitionService partitionService;
        private String memberId;

        TestServer(int number, boolean useMemberQueue) {

            this.number = number;
            this.hzInstance = Hazelcast.newHazelcastInstance();
            this.partitionService = hzInstance.getPartitionService();
            this.memberId = hzInstance.getCluster().getLocalMember().getUuid();


            if (useMemberQueue) {
                this.queue = new MemberQueue<Long>(QUEUE_NAME, hzInstance);
            } else {
                this.queue = hzInstance.getQueue(QUEUE_NAME);
            }
        }


        void add(long value) {
            queue.add(value);
        }


        Long poll() {
            return queue.poll();
        }


        boolean isOwner(long value) {

            Partition partition = partitionService.getPartition(value);
            return ((MemberQueue) queue).isPartitionOwner(partition.getPartitionId());
        }

        @Override
        public String toString() {
            return "TestServer{" +
                    "number=" + number +
                    ", queue=" + queue +
                    '}';
        }
    }

    //@Test
    public void testSeveralInstances() throws InterruptedException {

        boolean smartQueue = false;
        int amountOfTasks = 10000;
        int serversQuantity = 2;
        int clientsQuantity = 1;

        TestServer[] servers = new TestServer[serversQuantity];

        long startInit = System.currentTimeMillis();

        for (int i = 0; i < serversQuantity; i++) {
            servers[i] = new TestServer(i, smartQueue);
            System.out.println("server " + i + ": " + servers[i]);
        }


        System.out.println("Initialization completed: " + (System.currentTimeMillis() - startInit) + " mls.");
        System.out.println("Start to fill...");

        long startFill = System.currentTimeMillis();

        fillQueue(servers, smartQueue, amountOfTasks, clientsQuantity);

        System.out.println("Queue filled: " + (System.currentTimeMillis() - startFill) + " mls.");
        System.out.println("Start to poll...");


        long startPoll = System.currentTimeMillis();

        pollAll(servers, amountOfTasks, clientsQuantity);

        System.out.println("Poll completed: " + (System.currentTimeMillis() - startFill) + " mls.");

        // quickest shutdown!
        System.exit(-1);

    }

    private void fillQueue(TestServer[] testServer, boolean smartQueue, int amountOfTasks, int clientsQuantity) {

        int counter = 0;
        int successPut = 0;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < amountOfTasks; i++) {

            long newValue = random.nextLong(Long.MAX_VALUE);

            if (smartQueue) {

                for (int j = 0; j < testServer.length; j++) {

                    int robinChoose = counter++ % testServer.length;
                    if (testServer[robinChoose].isOwner(newValue)) {
                        testServer[robinChoose].add(newValue);
                        successPut++;
                        break;
                    }
                }

            } else {
                testServer[0].add(newValue);
                successPut++;
            }

        }

        assertEquals(amountOfTasks, successPut);
    }


    private void pollAll(TestServer[] testServer, int amountOfTasks, int clientsQuantity) {

        int counter = 0;
        int successPoll = 0;

        int emptyServers = 0;

        while (true) {
            int robinChoose = counter++ % testServer.length;

            TestServer server = testServer[robinChoose];

            if (server == null) {
                continue;
            }

            Long item = server.poll();

            if (item == null) {
                testServer[robinChoose] = null;
                if (++ emptyServers == testServer.length) {
                    break;
                } else {
                    continue;
                }

            }

            if (++successPoll == amountOfTasks) {
                break;
            }
        }


        assertEquals(amountOfTasks, successPoll);
    }
}
