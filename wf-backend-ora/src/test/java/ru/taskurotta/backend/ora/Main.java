package ru.taskurotta.backend.ora;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.runnable.CreationTask;

/**
 * User: greg
 */
public class Main {

    public static void main(String... args) throws Exception {
        final DbConnect dbConnect = new DbConnect();
        final Executor exec = Executors.newFixedThreadPool(9);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        exec.execute(new CreationTask(dbConnect.getDataSource()));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 0, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 1, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 2, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 3, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 0, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 1, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 2, countDownLatch));
//        exec.execute(new TestSelectTask(dbConnect.getDataSource(), 3, countDownLatch));
        countDownLatch.await();
        System.exit(0);
    }
}