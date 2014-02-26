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

    protected static final String SQL_GET_PROCESS_CNT_BY_STATE = "SELECT COUNT(PROCESS_ID) AS cnt FROM PROCESS WHERE STATE = ? ";

    @Override
    public int getFinishedCount() {
        return getFinishedCount(null);
    }

    @Override
    public int getFinishedCount(String customId) {
        int result = 0;
        String sql = customId!=null? SQL_GET_PROCESS_CNT_BY_STATE + " AND CUSTOM_ID = ? ": SQL_GET_PROCESS_CNT_BY_STATE;
        try (Connection connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, ru.taskurotta.service.console.model.Process.FINISH);
            if (customId!=null) {
                ps.setString(2, customId);
            }
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
