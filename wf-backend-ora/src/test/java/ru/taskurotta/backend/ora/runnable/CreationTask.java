package ru.taskurotta.backend.ora.runnable;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.backend.ora.domain.SimpleTask;
import ru.taskurotta.backend.ora.queue.OraQueueDao;

import java.util.UUID;

/**
 * User: greg
 */
public class CreationTask implements Runnable {

    private BasicDataSource dataSource;

    public CreationTask(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run() {
        OraQueueDao dbDAO = new OraQueueDao(dataSource);
        int count = 0;
        System.out.println("Starting adding data");
        while (count < 100000) {
            count++;
            dbDAO.enqueueTask(new SimpleTask(UUID.randomUUID(), UUID.randomUUID(), System.currentTimeMillis(), 0, "DEF"), "QUEUE_TEST");
        }
        System.out.println("Stopped adding data");
    }
}