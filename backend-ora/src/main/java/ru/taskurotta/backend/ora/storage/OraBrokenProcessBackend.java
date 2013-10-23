package ru.taskurotta.backend.ora.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.StringUtils;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.BrokenProcessVO;
import ru.taskurotta.backend.process.SearchCommand;
import ru.taskurotta.exception.BackendCriticalException;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 14.10.13 16:41
 */
public class OraBrokenProcessBackend extends JdbcDaoSupport implements BrokenProcessBackend {

    private static final Logger logger = LoggerFactory.getLogger(OraBrokenProcessBackend.class);

    protected static final String SQL_CREATE_BROKEN_PROCESS = "BEGIN " +
            "INSERT INTO TSK_BROKEN_PROCESSES (PROCESS_ID, START_ACTOR_ID, BROKEN_ACTOR_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME, STACK_TRACE) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING ID INTO ?; " +
            "END;";

    protected static final String SQL_LIST_ALL = "SELECT * FROM TSK_BROKEN_PROCESSES ";

    protected static final String SQL_DELETE_BROKEN_PROCESS = "DELETE FROM TSK_BROKEN_PROCESSES WHERE PROCESS_ID = ?";

    protected LobHandler lobHandler;

    protected RowMapper<BrokenProcessVO> brokenProcessRowMapper = new RowMapper<BrokenProcessVO>() {

        @Override
        public BrokenProcessVO mapRow(ResultSet rs, int i) throws SQLException {
            BrokenProcessVO result = new BrokenProcessVO();
            result.setBrokenActorId(rs.getString("BROKEN_ACTOR_ID"));
            result.setErrorClassName(rs.getString("ERROR_CLASS_NAME"));
            result.setProcessId(rs.getString("PROCESS_ID"));
            result.setStackTrace(lobHandler.getClobAsString(rs, "STACK_TRACE"));
            result.setErrorMessage(rs.getString("ERROR_MESSAGE"));
            result.setStartActorId(rs.getString("START_ACTOR_ID"));
            result.setTime(rs.getLong("TIME"));

            return result;
        }
    };

    @Override
    public void save (final BrokenProcessVO brokenProcessVO) {

        try {
            Long id = getJdbcTemplate().execute(SQL_CREATE_BROKEN_PROCESS,
                    new CallableStatementCallback<Long>() {

                        public Long doInCallableStatement(CallableStatement ps) throws SQLException, DataAccessException {
                            LobCreator lobCreator = lobHandler.getLobCreator();
                            ps.setString(1, brokenProcessVO.getProcessId());
                            ps.setString(2, brokenProcessVO.getStartActorId());
                            ps.setString(3, brokenProcessVO.getBrokenActorId());
                            ps.setTimestamp(4, new Timestamp(new Date().getTime()));
                            ps.setLong(5, brokenProcessVO.getTime());
                            ps.setString(6, brokenProcessVO.getErrorMessage());
                            ps.setString(7, brokenProcessVO.getErrorClassName());
                            lobCreator.setClobAsString(ps, 8, brokenProcessVO.getStackTrace());

                            ps.registerOutParameter(9, Types.BIGINT);

                            ps.execute();
                            lobCreator.close();

                            return ps.getLong(9);
                        }
                    });

            logger.debug("Created BrokenProcessVO entry with key[{}]", id);
        } catch (DataAccessException e) {
            String errMessage = "Cannot create BrokenProcessVO entry["+brokenProcessVO+"]";
            logger.error(errMessage, e);
            throw new BackendCriticalException(errMessage);
        }

    }

    @Override
    public Collection<BrokenProcessVO> find(SearchCommand searchCommand) {
        List<Object> parameters = new ArrayList<>();//order does matter
        StringBuilder sb = new StringBuilder(SQL_LIST_ALL);
        boolean first = true;
        if (StringUtils.hasText(searchCommand.getStartActorId())) {
            sb.append((first? " WHERE ":" AND ")).append("START_ACTOR_ID LIKE ? ");
            parameters.add(searchCommand.getStartActorId() + "%");
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getBrokenActorId())) {
            sb.append((first? " WHERE ":" AND ")).append("BROKEN_ACTOR_ID LIKE ? ");
            parameters.add(searchCommand.getBrokenActorId() + "%");
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            sb.append((first? " WHERE ":" AND ")).append("ERROR_CLASS_NAME LIKE ? ");
            parameters.add(searchCommand.getErrorClassName() + "%");
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            sb.append((first? " WHERE ":" AND ")).append("ERROR_MESSAGE LIKE ? ");
            parameters.add(searchCommand.getErrorMessage() + "%");
            first = false;
        }

        if (searchCommand.getEndPeriod()>0) {
            sb.append((first? " WHERE ":" AND ")).append("TIME < ? ");
            parameters.add(searchCommand.getEndPeriod());
            first = false;
        }

        if (searchCommand.getStartPeriod()>0) {
            sb.append((first? " WHERE ":" AND ")).append("TIME > ? ");
            parameters.add(searchCommand.getStartPeriod());
            first = false;
        }

        Collection<BrokenProcessVO> result;
        String searchSql = sb.toString();
        long startTime = System.currentTimeMillis();
        try {
            result = getJdbcTemplate().query(searchSql, parameters.toArray(), brokenProcessRowMapper);
        } catch(EmptyResultDataAccessException e) {
           result = Collections.emptyList();
        }

        logger.trace("SearchSQL getted[{}], params are[{}]", searchSql, parameters);
        logger.debug("Found [{}] result by command[{}] in [{}]ms", result.size(), searchCommand, (System.currentTimeMillis()-startTime));

        return result;
    }

    @Override
    public Collection<BrokenProcessVO> findAll() {
        Collection<BrokenProcessVO> result;
        try {
            result = getJdbcTemplate().query(SQL_LIST_ALL, brokenProcessRowMapper);
        } catch(EmptyResultDataAccessException e) {
            result = Collections.emptyList();//nothing found
        }

        logger.debug("Found [{}] broken processes", result.size());
        return result;
    }

    @Override
    public void delete(final String processId) {

        if (processId == null || StringUtils.isEmpty(processId)) {
            return;
        }

        getJdbcTemplate().update(SQL_DELETE_BROKEN_PROCESS, processId);
    }

    @Required
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }

}
