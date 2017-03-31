package ru.taskurotta.service.pg;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IdReturningCallback implements PreparedStatementCallback<Long> {

    @Override
    public Long doInPreparedStatement(PreparedStatement preparedStatement) throws SQLException, DataAccessException {
        preparedStatement.execute();
        ResultSet rs = preparedStatement.getResultSet();
        long result = -1l;
        while (rs.next()) {
            result = rs.getLong(1);
            break;
        }
        return result;
    }
}
