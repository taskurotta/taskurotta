package ru.taskurotta.restarter.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.restarter.ProcessVO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 16:51
 */
public class AnalyzerImpl implements Analyzer {

    private static Logger logger = LoggerFactory.getLogger(AnalyzerImpl.class);

    private DataSource dataSource;
    private int processBatchSize;

    @Override
    public List<ProcessVO> findNotFinishedProcesses(long fromTime) {
        logger.info("Try to find incomplete processes, was started before [{}] ({})", fromTime, new Date(fromTime));

        List<ProcessVO> processes = new ArrayList<>();

        String query = "SELECT * FROM (SELECT process_id, start_time, start_task_id FROM process p WHERE state = ? AND start_time < ? ORDER BY start_time) WHERE ROWNUM <= ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, 0);
            preparedStatement.setLong(2, fromTime);
            preparedStatement.setInt(3, processBatchSize);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                UUID processId = UUID.fromString(resultSet.getString("process_id"));
                long startTime = resultSet.getLong("start_time");
                UUID startTaskId = UUID.fromString(resultSet.getString("start_task_id"));

                ProcessVO process = new ProcessVO(processId, startTime, startTaskId);
                processes.add(process);

                logger.debug("Found incomplete processId [{}] started at [{}]({}) with taskId [{}]", processId, startTime, new Date(startTime), startTaskId);
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }

        logger.info("Found [{}] incomplete processes", processes.size());

        return processes;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setProcessBatchSize(int processBatchSize) {
        this.processBatchSize = processBatchSize;
    }
}
