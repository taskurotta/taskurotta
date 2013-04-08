package ru.taskurotta.backend.ora.runnable;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.backend.ora.dao.OraQueueDao;
import ru.taskurotta.backend.ora.domain.SimpleTask;

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
        try {
            System.out.println("Starting adding data");
            while (count < 100000) {
                count++;
                dbDAO.enqueueTask(new SimpleTask(UUID.randomUUID(), new Date(), 0, "DEF"), "QUEUE_TEST");
            }
            System.out.println("Stopped adding data");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}