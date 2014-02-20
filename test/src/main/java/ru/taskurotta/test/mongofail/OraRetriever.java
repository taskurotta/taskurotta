package ru.taskurotta.test.mongofail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: 20.02.14 12:04
 */
public class OraRetriever implements FinishedCountRetriever {

    private DataSource dataSource;

    private static final Logger log = LoggerFactory.getLogger(OraRetriever.class);

    @Override
    public int getFinishedCount() {
        int result = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT COUNT(PROCESS_ID) AS cnt FROM PROCESS WHERE STATE = ?")) {

            ps.setInt(1, ru.taskurotta.service.console.model.Process.FINISH);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getInt("cnt");
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
        return result;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
