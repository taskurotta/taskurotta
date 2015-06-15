package ru.taskurotta.service.ora.notification.storage;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.lob.LobHandler;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.dao.NotificationDao;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.service.ora.OracleQueryUtils;
import ru.taskurotta.util.NotificationUtils;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

/**
 * Created on 11.06.2015.
 */
public class OraNotificationDao extends JdbcDaoSupport implements NotificationDao {

    private LobHandler lobHandler;

    private static final String SQL_GET_SUBSCRIPTION_BY_ID = "select * from TSK_NFN_SUBSCRIPTIONS where ID = ? ";
    private static final String SQL_LIST_SUBSCRIPTIONS = "select * from TSK_NFN_SUBSCRIPTIONS";
    private static final String SQL_GET_TRIGGER_BY_ID = "select * from TSK_NFN_TRIGGERS where ID = ? ";
    private static final String SQL_LIST_TRIGGERS = "select * from TSK_NFN_TRIGGERS";
    private static final String SQL_ADD_SUBSCRIPTION = "begin insert into TSK_NFN_SUBSCRIPTIONS (ACTORS_JSON, EMAILS_JSON, CHANGE_DATE) values (?, ?, ?) returning ID into ?; end;";
    private static final String SQL_ADD_TRIGGER = "begin insert into TSK_NFN_TRIGGERS (TYPE, STATE_JSON, CFG_JSON, CHANGE_DATE) values (?, ?, ?, ?) returning ID into ?; end;";
    private static final String SQL_INSERT_LINK = "insert into TSK_NFN_LINKS (SUBSCRIPTION_ID, TRIGGER_ID) values (?, ?) ";
    private static final String SQL_UPDATE_SUBSCRIPTION = "update TSK_NFN_SUBSCRIPTIONS set ACTORS_JSON = ?, EMAILS_JSON = ?, CHANGE_DATE = ? where ID = ? ";
    private static final String SQL_UPDATE_TRIGGER = "update TSK_NFN_TRIGGERS set TYPE = ?, STATE_JSON = ?, CFG_JSON = ?, CHANGE_DATE = ? where ID = ? ";
    private static final String SQL_GET_SUBSCRIPTIONS_BY_TRIGGER_ID = "select * from TSK_NFN_SUBSCRIPTIONS where ID in (select SUBSCRIPTION_ID from TSK_NFN_LINKS where TRIGGER_ID = ?)";
    private static final String SQL_LIST_TRIGGER_KEYS = "select ID from TSK_NFN_TRIGGERS";
    private static final String SQL_LIST_TRIGGER_KEYS_BY_SUBSCRIPTION = "select TRIGGER_ID as id from TSK_NFN_LINKS where SUBSCRIPTION_ID = ? ";

    private static final String SQL_DELETE_SUBSCRIPTION = "delete from TSK_NFN_SUBSCRIPTIONS where ID = ? ";
    private static final String SQL_DELETE_SUBSCRIPTION_LINKS = "delete from TSK_NFN_LINKS where SUBSCRIPTION_ID = ? ";

    private RowMapper<Long> keyMapper = new RowMapper<Long>() {
        @Override
        public Long mapRow(ResultSet resultSet, int i) throws SQLException {
            return resultSet.getLong("ID");
        }
    };

    private RowMapper<Subscription> subscriptionMapper = new RowMapper<Subscription>() {
        @Override
        public Subscription mapRow(ResultSet rs, int i) throws SQLException {
            Subscription result = new Subscription();
            result.setId(rs.getLong("ID"));

            String actorsJson = lobHandler.getClobAsString(rs, "ACTORS_JSON");
            result.setActorIds(NotificationUtils.jsonToList(actorsJson, null));

            String emailsJson = lobHandler.getClobAsString(rs, "EMAILS_JSON");
            result.setEmails(NotificationUtils.jsonToList(emailsJson, null));

            result.setChangeDate(rs.getTimestamp("CHANGE_DATE"));
            return result;
        }
    };

    private RowMapper<NotificationTrigger> triggerMapper = new RowMapper<NotificationTrigger>() {
        @Override
        public NotificationTrigger mapRow(ResultSet rs, int rowNum) throws SQLException {
            NotificationTrigger result = new NotificationTrigger();
            result.setId(rs.getLong("ID"));
            result.setType(rs.getString("TYPE"));
            result.setStoredState(lobHandler.getClobAsString(rs, "STATE_JSON"));
            result.setCfg(lobHandler.getClobAsString(rs, "CFG_JSON"));
            result.setChangeDate(rs.getTimestamp("CHANGE_DATE"));

            return result;
        }
    };

    @Override
    public Subscription getSubscription(long id) {
        try {
            Subscription result = getJdbcTemplate().queryForObject(SQL_GET_SUBSCRIPTION_BY_ID, subscriptionMapper, id);
            List<Long> triggerKeys = getJdbcTemplate().query(SQL_LIST_TRIGGER_KEYS_BY_SUBSCRIPTION, keyMapper);
            result.setTriggersKeys(triggerKeys);
            return result;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public NotificationTrigger getTrigger(long id) {
        try {
            return getJdbcTemplate().queryForObject(SQL_GET_TRIGGER_BY_ID, triggerMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public long addSubscription(final Subscription subscription) {
        long result = getJdbcTemplate().execute(SQL_ADD_SUBSCRIPTION, new CallableStatementCallback<Long>() {
            @Override
            public Long doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                cs.setString(1, NotificationUtils.listToJson(subscription.getActorIds(), "[]"));
                cs.setString(2, NotificationUtils.listToJson(subscription.getEmails(), "[]"));
                cs.setTimestamp(3, new Timestamp(subscription.getChangeDate().getTime()));
                cs.registerOutParameter(4, Types.BIGINT);
                cs.execute();

                return cs.getLong(4);
            }
        });
        if (subscription.getTriggersKeys() != null) {
            for (Long triggerKey : subscription.getTriggersKeys()) {
                insertLink(result, triggerKey);
            }
        }
        return result;
    }

    @Override
    public void removeSubscription(long id) {
        getJdbcTemplate().update(SQL_DELETE_SUBSCRIPTION_LINKS, id);
        getJdbcTemplate().update(SQL_DELETE_SUBSCRIPTION, id);
    }

    @Override
    public long addTrigger(final NotificationTrigger trigger) {
        return getJdbcTemplate().execute(SQL_ADD_TRIGGER, new CallableStatementCallback<Long>() {
            @Override
            public Long doInCallableStatement(CallableStatement cs) throws SQLException, DataAccessException {
                cs.setString(1, trigger.getType());
                cs.setString(2, trigger.getStoredState());
                cs.setString(3, trigger.getCfg());
                cs.setTimestamp(4, new Timestamp(trigger.getChangeDate().getTime()));

                cs.registerOutParameter(3, Types.BIGINT);
                cs.execute();
                return cs.getLong(3);
            }
        });
    }

    public void insertLink(long subscriptionId, long triggerId) {
        getJdbcTemplate().update(SQL_INSERT_LINK, subscriptionId, triggerId);
    }

    @Override
    public void updateSubscription(Subscription subscription, long id) {
        getJdbcTemplate().update(SQL_UPDATE_SUBSCRIPTION, subscription.getActorIds(), subscription.getEmails(), subscription.getChangeDate(), id);
    }

    @Override
    public void updateTrigger(NotificationTrigger trigger, long id) {
        getJdbcTemplate().update(SQL_UPDATE_TRIGGER, trigger.getType(), trigger.getStoredState(), trigger.getCfg(), trigger.getChangeDate(), id);
    }

    @Override
    public Collection<Subscription> listSubscriptions() {
        return getJdbcTemplate().query(SQL_LIST_SUBSCRIPTIONS, subscriptionMapper);
    }

    @Override
    public GenericPage<Subscription> listSubscriptions(SearchCommand command) {
        int startIndex = (command.getPageNum() - 1) * command.getPageSize() + 1;
        int endIndex = startIndex + command.getPageSize() - 1;
        final long[] totalCnt = new long[1];//container to provide access to final var
        totalCnt[0] = 0l;
        List<Subscription> items = getJdbcTemplate().query(OracleQueryUtils.createPagesQuery(SQL_LIST_SUBSCRIPTIONS), new RowMapper<Subscription>() {
            @Override
            public Subscription mapRow(ResultSet rs, int rowNum) throws SQLException {
                totalCnt[0] = rs.getLong("cnt");
                return subscriptionMapper.mapRow(rs, rowNum);
            }
        }, endIndex, startIndex);

        return new GenericPage<>(items, command.getPageNum(), command.getPageSize(), totalCnt[0]);
    }

    @Override
    public Collection<NotificationTrigger> listTriggers() {
        return getJdbcTemplate().query(SQL_LIST_TRIGGERS, triggerMapper);
    }

    @Override
    public Collection<Subscription> listTriggerSubscriptions(NotificationTrigger trigger) {
        return getJdbcTemplate().query(SQL_GET_SUBSCRIPTIONS_BY_TRIGGER_ID, subscriptionMapper, trigger.getId());
    }

    @Override
    public Collection<Long> listTriggerKeys() {
        return getJdbcTemplate().query(SQL_LIST_TRIGGER_KEYS, keyMapper);
    }

    @Required
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }
}
