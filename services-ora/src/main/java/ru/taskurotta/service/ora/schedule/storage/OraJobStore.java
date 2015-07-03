package ru.taskurotta.service.ora.schedule.storage;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.schedule.storage.JobStore;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;

/**
 * Oracle implementation of Scheduled tasks storage
 * User: dimadin
 * Date: 24.09.13 15:35
 */
public class OraJobStore extends JdbcDaoSupport implements JobStore {

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    private static final String SQL_ADD_JOB = "BEGIN INSERT INTO TSK_SCHEDULED (NAME, CRON, STATUS, JSON, CREATED, QUEUE_LIMIT, MAX_ERRORS, ERR_COUNT) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING ID INTO ?; END;";
    private static final String SQL_DELETE_JOB = "DELETE FROM TSK_SCHEDULED WHERE ID = ? ";
    private static final String SQL_GET_KEYS = "SELECT ID FROM TSK_SCHEDULED";
    private static final String SQL_GET_JOB_BY_ID = "SELECT * FROM TSK_SCHEDULED WHERE id = ? ";
    private static final String SQL_LIST_JOBS = "SELECT * FROM TSK_SCHEDULED";
    private static final String SQL_UPDATE_JOB_STATUS = "UPDATE TSK_SCHEDULED SET STATUS = ? WHERE id = ? ";
    private static final String SQL_UPDATE_JOB = "UPDATE TSK_SCHEDULED SET NAME = ?, CRON = ?, STATUS = ?, JSON = ?, CREATED = ?, QUEUE_LIMIT = ?, MAX_ERRORS = ? WHERE id = ? ";
    private static final String SQL_GET_JOB_STATUS = "SELECT status FROM TSK_SCHEDULED WHERE id = ? ";
    private static final String SQL_UPDATE_ERROR_COUNT = "UPDATE TSK_SCHEDULED SET ERR_COUNT = ?, LAST_ERR_MESSAGE = ? WHERE id = ? ";

    private RowMapper<Long> keyMapper = new RowMapper<Long>() {
        @Override
        public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getLong("ID");
        }
    };

    private RowMapper<Integer> statusMapper = new RowMapper<Integer>() {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("STATUS");
        }
    };

    private RowMapper<JobVO> jobMapper = new RowMapper<JobVO>() {
        @Override
        public JobVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            JobVO result = new JobVO();
            result.setId(rs.getLong("ID"));
            result.setName(rs.getString("NAME"));
            result.setCron(rs.getString("CRON"));
            result.setStatus(rs.getInt("STATUS"));
            result.setQueueLimit(rs.getInt("QUEUE_LIMIT"));
            result.setMaxErrors(rs.getInt("MAX_ERRORS"));
            result.setErrorCount(rs.getInt("ERR_COUNT"));
            result.setLastError(rs.getString("LAST_ERR_MESSAGE"));
            result.setTask(taskSerializer.deserialize(rs.getString("JSON")));

            return result;
        }
    };


    @Override
    public long add(final JobVO task) {
        return getJdbcTemplate().execute(SQL_ADD_JOB, new CallableStatementCallback<Long>() {
            @Override
            public Long doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                cs.setString(1, task.getName());
                cs.setString(2, task.getCron());
                cs.setInt(3, task.getStatus());
                cs.setString(4, (String) taskSerializer.serialize(task.getTask()));
                cs.setTimestamp(5, new Timestamp(new Date().getTime()));
                cs.setInt(6, task.getQueueLimit());
                cs.setInt(7, task.getMaxErrors());
                cs.setInt(8, task.getErrorCount());

                cs.registerOutParameter(9, Types.NUMERIC);
                cs.execute();

                return cs.getLong(9);
            }
        });
    }

    @Override
    public void remove(long id) {
        getJdbcTemplate().update(SQL_DELETE_JOB, id);
    }

    @Override
    public Collection<Long> getKeys() {
        return getJdbcTemplate().query(SQL_GET_KEYS, keyMapper);
    }

    @Override
    public JobVO get(long id) {
        try {
            return getJdbcTemplate().queryForObject(SQL_GET_JOB_BY_ID, jobMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<JobVO> getAll() {
        return getJdbcTemplate().query(SQL_LIST_JOBS, jobMapper);
    }

    @Override
    public void updateJobStatus(long id, int status) {
        getJdbcTemplate().update(SQL_UPDATE_JOB_STATUS, status, id);
    }

    @Override
    public void update(JobVO jobVO, long id) {
        getJdbcTemplate().update(SQL_UPDATE_JOB, jobVO.getName(), jobVO.getCron(), jobVO.getStatus(), (String) taskSerializer.serialize(jobVO.getTask()), new Date(), jobVO.getQueueLimit(), jobVO.getMaxErrors(), id);
    }

    @Override
    public int getJobStatus(long jobId) {
        return getJdbcTemplate().queryForObject(SQL_GET_JOB_STATUS, statusMapper, jobId);
    }

    @Override
    public void updateErrorCount(long jobId, int count, String message) {
        getJdbcTemplate().update(SQL_UPDATE_ERROR_COUNT, count, message, jobId);
    }


}
