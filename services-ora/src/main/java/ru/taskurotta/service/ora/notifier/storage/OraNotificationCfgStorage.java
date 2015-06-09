package ru.taskurotta.service.ora.notifier.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.lob.LobHandler;
import ru.taskurotta.service.notification.model.NotificationConfig;
import ru.taskurotta.service.storage.EntityStore;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created 09/06/15.
 */
public class OraNotificationCfgStorage extends JdbcDaoSupport implements EntityStore<NotificationConfig>{

    private ObjectMapper mapper = new ObjectMapper();

    private LobHandler lobHandler;

    private static final String SQL_GET_ALL_KEYS  = "select id from TSK_NOTIFICATION_CFG";
    private static final String SQL_LIST_NOTIF_CFG  = "select * from TSK_NOTIFICATION_CFG";
    private static final String SQL_GET_NOTIF_CFG_BY_ID = "select * from TSK_NOTIFICATION_CFG WHERE ID = ? ";
    private static final String SQL_REMOVE_NOTIF_CFG_BY_ID = "delete from TSK_NOTIFICATION_CFG WHERE ID = ? ";
    private static final String SQL_ADD_NOTIF_CFG = "BEGIN " +
            "INSERT INTO TSK_NOTIFICATION_CFG (TYPE, ACTORS_JSON, EMAILS_JSON, CHANGE_DATE) " +
            "VALUES (?, ?, ?, ?) " +
            "RETURNING ID INTO ?; END;";

    private static final String SQL_UPDATE_NOTIF_CFG = "update TSK_NOTIFICATION_CFG SET TYPE = ?, ACTORS_JSON = ?, EMAILS_JSON = ?, CHANGE_DATE = ? WHERE ID = ? ";

    protected RowMapper<Long> keyMapper = new RowMapper<Long>() {
        @Override
        public Long mapRow(ResultSet resultSet, int i) throws SQLException {
            return resultSet.getLong("ID");
        }
    };

    protected RowMapper<NotificationConfig> notificationCfgMapper = new RowMapper<NotificationConfig>() {
        @Override
        public NotificationConfig mapRow(ResultSet rs, int i) throws SQLException {
            NotificationConfig result = new NotificationConfig();
            result.setId(rs.getLong("ID"));
            result.setType(rs.getString("TYPE"));
            String actorsJson = lobHandler.getClobAsString(rs, "ACTORS_JSON");
            if (actorsJson != null) {
                try {
                    result.setActorIds((List<String>) mapper.readValue(actorsJson, new TypeReference<List<String>> (){}));
                } catch (Exception e) {
                    throw new RuntimeException("Cannot parse actor id values from json ["+actorsJson+"]", e);
                }
            }
            String emailsJson = lobHandler.getClobAsString(rs, "EMAILS_JSON");
            if (emailsJson != null) {
                try {
                    result.setEmails((List<String>) mapper.readValue(emailsJson, new TypeReference<List<String>> (){}));
                } catch (Exception e) {
                    throw new RuntimeException("Cannot parse emails from json ["+emailsJson+"]", e);
                }
            }
            return result;
        }
    };


    @Override
    public long add(final NotificationConfig value) {
        return getJdbcTemplate().execute(SQL_ADD_NOTIF_CFG, new CallableStatementCallback<Long>() {
            @Override
            public Long doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {

                cs.setString(1, value.getType());
                cs.setString(2, listAsJson(value.getActorIds()));
                cs.setString(3, listAsJson(value.getEmails()));
                cs.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

                cs.registerOutParameter(5, Types.NUMERIC);
                cs.execute();
                return cs.getLong(5);
            }
        });
    }

    private String listAsJson(List<String> target) {
        try {
            return mapper.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot parse list values to json: ["+target+"]", e);
        }
    }

    @Override
    public void remove(long id) {
        getJdbcTemplate().update(SQL_REMOVE_NOTIF_CFG_BY_ID, id);
    }

    @Override
    public void update(NotificationConfig entity, long id) {
        getJdbcTemplate().update(SQL_UPDATE_NOTIF_CFG, entity.getType(), listAsJson(entity.getActorIds()), listAsJson(entity.getEmails()), new Date(), id);
    }

    @Override
    public Collection<Long> getKeys() {
        return getJdbcTemplate().query(SQL_GET_ALL_KEYS, keyMapper);
    }

    @Override
    public NotificationConfig get(long id) {
        try {
            return getJdbcTemplate().queryForObject(SQL_GET_NOTIF_CFG_BY_ID, notificationCfgMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<NotificationConfig> getAll() {
        return getJdbcTemplate().query(SQL_LIST_NOTIF_CFG, notificationCfgMapper);
    }

    @Required
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }
}
