package ru.taskurotta.oracle.test.runnable;

import org.apache.commons.dbcp.BasicDataSource;
import ru.taskurotta.oracle.test.DbDAO;
import ru.taskurotta.oracle.test.GenerationTools;
import ru.taskurotta.oracle.test.domain.SimpleTask;

import java.sql.SQLException;
import java.util.Date;

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
            DbDAO dbDAO = new DbDAO(dataSource);
            int count = 0;
            try {
                System.out.println("Starting adding data");
                while (count < 50000) {
                    count++;
                    dbDAO.insertData(new SimpleTask(count, GenerationTools.getRandomType(), new Date(), 0));
                }
                System.out.println("Stopped adding data");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }