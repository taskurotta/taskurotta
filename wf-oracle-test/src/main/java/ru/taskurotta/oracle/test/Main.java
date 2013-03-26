package ru.taskurotta.oracle.test;

import ru.taskurotta.oracle.test.runnable.TestSelectTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * User: greg
 */
public class Main {

    public static void main(String... args) throws Exception {
        final DbConnect dbConnect = new DbConnect();
        final Executor exec = Executors.newFixedThreadPool(9);
        final CountDownLatch countDownLatch = new CountDownLatch(8);
//        exec.execute(new CreationTask(dbConnect.getDataSource()));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 0, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 1, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 2, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 3, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 0, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 1, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 2, countDownLatch));
        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 3, countDownLatch));
        countDownLatch.await();
        System.exit(0);
    }
}
