package ru.taskurotta.service.pg.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import ru.taskurotta.service.pg.IdReturningCallback;
import ru.taskurotta.service.pg.PgQueryUtils;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.schedule.storage.JobStore;
import ru.taskurotta.transport.model.TaskContainer;

import java.sql.*;
import java.util.Collection;
import java.util.Date;

public class PgSchedulerJobStore extends JdbcDaoSupport implements JobStore {

    private static final String SQL_ADD_JOB = "INSERT INTO TSK_SCHEDULED (NAME, CRON, STATUS, JSON, CREATED, LIMIT_CNT, MAX_ERRORS, ERR_COUNT, LAST_ERR_MESSAGE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING ID ";
    private static final String SQL_DELETE_JOB = "DELETE FROM TSK_SCHEDULED WHERE ID = ? ";
    private static final String SQL_GET_KEYS = "SELECT ID FROM TSK_SCHEDULED";
    private static final String SQL_GET_JOB_BY_ID = "SELECT * FROM TSK_SCHEDULED WHERE id = ? ";
    private static final String SQL_LIST_JOBS = "SELECT * FROM TSK_SCHEDULED";
    private static final String SQL_UPDATE_JOB_STATUS = "UPDATE TSK_SCHEDULED SET STATUS = ? WHERE id = ? ";
    private static final String SQL_UPDATE_JOB = "UPDATE TSK_SCHEDULED SET NAME = ?, CRON = ?, STATUS = ?, JSON = ?, CREATED = ?, LIMIT_CNT = ?, MAX_ERRORS = ? WHERE id = ? ";
    private static final String SQL_GET_JOB_STATUS = "SELECT status FROM TSK_SCHEDULED WHERE id = ? ";
    private static final String SQL_UPDATE_ERROR_COUNT = "UPDATE TSK_SCHEDULED SET ERR_COUNT = ?, LAST_ERR_MESSAGE = ? WHERE id = ? ";

    private RowMapper<Long> keyMapper = (rs, rowNum) -> rs.getLong("ID");

    private RowMapper<Integer> statusMapper = (rs, rowNum) -> rs.getInt("STATUS");

    private RowMapper<JobVO> jobMapper = (rs, rowNum) -> {
        JobVO result = new JobVO();
        result.setId(rs.getLong("ID"));
        result.setName(rs.getString("NAME"));
        result.setCron(rs.getString("CRON"));
        result.setStatus(rs.getInt("STATUS"));
        result.setLimit(rs.getInt("LIMIT_CNT"));
        result.setMaxErrors(rs.getInt("MAX_ERRORS"));
        result.setErrorCount(rs.getInt("ERR_COUNT"));
        result.setLastError(rs.getString("LAST_ERR_MESSAGE"));
        result.setTask(PgQueryUtils.readValue(rs.getString("JSON"), TaskContainer.class));
        result.setLastError(rs.getString("LAST_ERR_MESSAGE"));
        return result;
    };

    @Override
    public long add(final JobVO task) {
        return getJdbcTemplate().execute(con -> {
            PreparedStatement ps = con.prepareStatement(SQL_ADD_JOB);
            ps.setString(1, task.getName());
            ps.setString(2, task.getCron());
            ps.setInt(3, task.getStatus());
            ps.setObject(4, PgQueryUtils.asJsonbObject(task.getTask()));
            ps.setTimestamp(5, new Timestamp(new Date().getTime()));
            ps.setInt(6, task.getLimit());
            ps.setInt(7, task.getMaxErrors());
            ps.setInt(8, task.getErrorCount());
            ps.setString(9, task.getLastError());
            return ps;
        }, new IdReturningCallback());
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
        getJdbcTemplate().update(SQL_UPDATE_JOB, jobVO.getName(), jobVO.getCron(), jobVO.getStatus(), PgQueryUtils.asJsonbObject(jobVO.getTask()), new Date(), jobVO.getLimit(), jobVO.getMaxErrors(), id);
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
