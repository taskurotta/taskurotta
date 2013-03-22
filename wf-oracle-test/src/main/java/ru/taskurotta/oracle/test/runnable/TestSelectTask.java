package ru.taskurotta.oracle.test.runnable;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.oracle.test.DbDAO;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * User: greg
 */
public class TestSelectTask implements Runnable {

    private BasicDataSource dataSource;
    private int jobType;
    private CountDownLatch countDownLatch;

    public TestSelectTask(BasicDataSource dataSource, int jobType, CountDownLatch countDownLatch) {
        this.dataSource = dataSource;
        this.jobType = jobType;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        final UUID threadId = UUID.randomUUID();
        System.out.println("Thread started for " + threadId + ": " + new Date());
        int count = 0;
        while (count < 500) {
            count++;
            final DbDAO dbDAO = new DbDAO(dataSource);
            try {
                dbDAO.selectLastTaskWithTypeImproved(jobType);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        countDownLatch.countDown();
        System.out.println("Thread ID:" + threadId + " finished at " + new Date());

    }
}
