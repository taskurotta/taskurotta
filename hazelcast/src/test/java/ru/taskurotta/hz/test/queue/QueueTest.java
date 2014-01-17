package ru.taskurotta.hz.test.queue;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.hazelcast.HzQueueConfigSupport;

/**
 * User: stukushin
 * Date: 16.01.14
 * Time: 18:31
 */
public class QueueTest {

    private final String queueName = "testQueue";

    @Ignore
    @Test
    public void fakeStoreTest() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/ru.taskurotta.hz.test.queue/hz-queue-fake.xml");
        HazelcastInstance hazelcastInstance = applicationContext.getBean("hzInstance", HazelcastInstance.class);

        IQueue<Integer> iQueue = hazelcastInstance.getQueue(queueName);

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            iQueue.add(i);

            if (i % 10000 == 0) {
                System.out.println("Added [" + i + "] elements");
            }
        }
    }

    @Ignore
    @Test
    public void memoryStoreTest() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/ru.taskurotta.hz.test.queue/hz-queue-memory.xml");
        HazelcastInstance hazelcastInstance = applicationContext.getBean("hzInstance", HazelcastInstance.class);
        MemoryQueueStore queueStore = applicationContext.getBean("queueStore", MemoryQueueStore.class);

        IQueue<Integer> iQueue = hazelcastInstance.getQueue(queueName);

        for (int i = 0; i < 20; i++) {
            iQueue.add(i);
        }

        int memoryLimit= Integer.parseInt(hazelcastInstance.getConfig().getQueueConfig(queueName).getQueueStoreConfig().getProperty("memory-limit"));

        System.out.println("Queue size: [" + iQueue.size() + "], store size: [" + queueStore.size() + "], memory-limit: [" + memoryLimit + "]");

        queueStore.clear();

        System.out.println("After clear: queue size: [" + iQueue.size() + "], store size: [" + queueStore.size() + "]");

        while (!iQueue.isEmpty()) {
            System.out.println(iQueue.poll());
        }
    }

    @Ignore
    @Test
    public void mongoStoreTest() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/ru.taskurotta.hz.test.queue/hz-queue-mongo.xml");
        HazelcastInstance hazelcastInstance = applicationContext.getBean("hzInstance", HazelcastInstance.class);
        HzQueueConfigSupport hzQueueConfigSupport = applicationContext.getBean("queueConfigSupport", HzQueueConfigSupport.class);

        hzQueueConfigSupport.createQueueConfig(queueName);

        IQueue<Integer> iQueue = hazelcastInstance.getQueue(queueName);

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            iQueue.add(i);

            if (i % 100000 == 0) {
                System.out.println("Added [" + i + "] elements");
            }
        }
    }
}
