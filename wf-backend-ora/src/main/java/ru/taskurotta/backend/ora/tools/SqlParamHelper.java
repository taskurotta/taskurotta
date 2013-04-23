package ru.taskurotta.backend.ora.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * User: greg
 */
public final class SqlParamHelper {

    private SqlParamHelper(){

    }

    public static PreparedStatement createPreparedStatementWithSqlParams(Connection connection, List<SqlParam1> sqlParams, String query) throws SQLException {
        final PreparedStatement ps = connection.prepareStatement(query);
        for (SqlParam1 param : sqlParams) {
            if (param.getLongParam() != -1) {
                ps.setLong(param.getIndex(), param.getLongParam());
            } else {
                ps.setString(param.getIndex(), param.getStringParam());
            }
        }
        return ps;
    }
}
