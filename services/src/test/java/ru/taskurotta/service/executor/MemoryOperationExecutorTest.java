package ru.taskurotta.service.executor;

import org.junit.Test;
import ru.taskurotta.service.executor.mem.MemoryOperationExecutor;

import java.util.concurrent.CountDownLatch;

public class MemoryOperationExecutorTest {



    public static class TestOperation implements Operation {

        CountDownLatch cdl;

        @Override
        public void init(Object nativePoint) {
            cdl = (CountDownLatch) nativePoint;
        }

        @Override
        public void run() {
            cdl.countDown();
        }
    }


    @Test
    public void test() throws InterruptedException {

        int counter = 1000;

        CountDownLatch cdl = new CountDownLatch(counter);

        OperationExecutor opExecutor = new MemoryOperationExecutor("test", cdl, counter*10, 2);

        Operation op = new TestOperation();

        for (int i = 0; i < counter; i++) {
            opExecutor.enqueue(op);
        }

        cdl.await();

    }
}
