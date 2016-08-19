package ru.taskurotta.service.pg.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.dao.NotificationDao;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.service.pg.IdReturningCallback;
import ru.taskurotta.service.pg.PgQueryUtils;

import java.sql.*;
import java.util.Collection;
import java.util.List;

public class PgNotificationDao extends JdbcDaoSupport implements NotificationDao {

    private static final String SQL_GET_SUBSCRIPTION_BY_ID = "select * from TSK_NFN_SUBSCRIPTIONS where ID = ? ";
    private static final String SQL_LIST_SUBSCRIPTIONS = "select * from TSK_NFN_SUBSCRIPTIONS ";
    private static final String SQL_COUNT_SUBSCRIPTIONS = "select count(id) from TSK_NFN_SUBSCRIPTIONS ";
    private static final String SQL_GET_TRIGGER_BY_ID = "select * from TSK_NFN_TRIGGERS where ID = ? ";
    private static final String SQL_LIST_TRIGGERS = "select * from TSK_NFN_TRIGGERS ";
    private static final String SQL_ADD_SUBSCRIPTION = "insert into TSK_NFN_SUBSCRIPTIONS (ACTORS_JSON, EMAILS_JSON, CHANGE_DATE) values (?, ?, ?) returning ID ";
    private static final String SQL_ADD_TRIGGER = "insert into TSK_NFN_TRIGGERS (TYPE, STATE_JSON, CFG_JSON, CHANGE_DATE) values (?, ?, ?, ?) returning ID ";
    private static final String SQL_INSERT_LINK = "insert into TSK_NFN_LINKS (SUBSCRIPTION_ID, TRIGGER_ID) values (?, ?) ";
    private static final String SQL_UPDATE_SUBSCRIPTION = "update TSK_NFN_SUBSCRIPTIONS set ACTORS_JSON = ?, EMAILS_JSON = ?, CHANGE_DATE = ? where ID = ? ";
    private static final String SQL_UPDATE_TRIGGER = "update TSK_NFN_TRIGGERS set TYPE = ?, STATE_JSON = ?, CFG_JSON = ?, CHANGE_DATE = ? where ID = ? ";
    private static final String SQL_GET_SUBSCRIPTIONS_BY_TRIGGER_ID = "select * from TSK_NFN_SUBSCRIPTIONS where ID in (select SUBSCRIPTION_ID from TSK_NFN_LINKS where TRIGGER_ID = ?) ";
    private static final String SQL_LIST_TRIGGER_KEYS = "select ID from TSK_NFN_TRIGGERS ";
    private static final String SQL_LIST_TRIGGER_KEYS_BY_SUBSCRIPTION = "select TRIGGER_ID as id from TSK_NFN_LINKS where SUBSCRIPTION_ID = ? ";

    private static final String SQL_DELETE_SUBSCRIPTION = "delete from TSK_NFN_SUBSCRIPTIONS where ID = ? ";
    private static final String SQL_DELETE_SUBSCRIPTION_LINKS = "delete from TSK_NFN_LINKS where SUBSCRIPTION_ID = ? ";

    private RowMapper<Subscription> subscriptionMapper = (rs, i) -> {
        Subscription result = new Subscription();
        result.setId(rs.getLong("ID"));
        result.setActorIds(PgQueryUtils.readListValue(rs.getString("ACTORS_JSON"), String.class));
        result.setEmails(PgQueryUtils.readListValue(rs.getString("EMAILS_JSON"), String.class));
        result.setChangeDate(rs.getTimestamp("CHANGE_DATE"));
        return result;
    };

    private RowMapper<NotificationTrigger> triggerMapper = (rs, rowNum) -> {
        NotificationTrigger result = new NotificationTrigger();
        result.setId(rs.getLong("ID"));
        result.setType(rs.getString("TYPE"));
        result.setStoredState(rs.getString("STATE_JSON"));
        result.setCfg(rs.getString("CFG_JSON"));
        result.setChangeDate(rs.getTimestamp("CHANGE_DATE"));

        return result;
    };

    @Override
    public Subscription getSubscription(long id) {
        try {
            Subscription result = getJdbcTemplate().queryForObject(SQL_GET_SUBSCRIPTION_BY_ID, subscriptionMapper, id);
            List<Long> triggerKeys = getJdbcTemplate().queryForList(SQL_LIST_TRIGGER_KEYS_BY_SUBSCRIPTION, Long.class, id);
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

        long result = getJdbcTemplate().execute(con -> {
            PreparedStatement ps = con.prepareStatement(SQL_ADD_SUBSCRIPTION);
            ps.setObject(1, PgQueryUtils.asJsonbObject(subscription.getActorIds()));
            ps.setObject(2, PgQueryUtils.asJsonbObject(subscription.getEmails()));
            ps.setTimestamp(3, new Timestamp(subscription.getChangeDate().getTime()));
            return ps;
        }, new IdReturningCallback());

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

        return getJdbcTemplate().execute(con -> {
            PreparedStatement ps = con.prepareStatement(SQL_ADD_TRIGGER);
            ps.setString(1, trigger.getType());
            ps.setObject(2, PgQueryUtils.jsonAsObject(trigger.getStoredState()));
            ps.setObject(3, PgQueryUtils.jsonAsObject(trigger.getCfg()));
            ps.setTimestamp(4, new Timestamp(trigger.getChangeDate().getTime()));

            return ps;
        }, new IdReturningCallback());

    }

    public void insertLink(long subscriptionId, long triggerId) {
        getJdbcTemplate().update(SQL_INSERT_LINK, subscriptionId, triggerId);
    }

    @Override
    public void updateSubscription(Subscription subscription, long id) {
        getJdbcTemplate().update(SQL_UPDATE_SUBSCRIPTION, PgQueryUtils.asJsonbObject(subscription.getActorIds()), PgQueryUtils.asJsonbObject(subscription.getEmails()), subscription.getChangeDate(), id);
        getJdbcTemplate().update(SQL_DELETE_SUBSCRIPTION_LINKS, id);
        if (subscription.getTriggersKeys() != null) {
            for (long triggerKey : subscription.getTriggersKeys()) {
                insertLink(id, triggerKey);
            }
        }
    }

    @Override
    public void updateTrigger(NotificationTrigger trigger, long id) {
        getJdbcTemplate().update(SQL_UPDATE_TRIGGER, trigger.getType(), PgQueryUtils.jsonAsObject(trigger.getStoredState()), PgQueryUtils.jsonAsObject(trigger.getCfg()), trigger.getChangeDate(), id);
    }

    @Override
    public Collection<Subscription> listSubscriptions() {
        return getJdbcTemplate().query(SQL_LIST_SUBSCRIPTIONS, subscriptionMapper);
    }

    @Override
    public GenericPage<Subscription> listSubscriptions(SearchCommand command) {
        long count = getJdbcTemplate().queryForObject(SQL_COUNT_SUBSCRIPTIONS, Long.class);
        long offset = Long.valueOf(Math.max(0, command.getPageNum()-1)) * Long.valueOf(Math.max(0, command.getPageSize()));
        List<Subscription> items = getJdbcTemplate().query(SQL_LIST_SUBSCRIPTIONS + "OFFSET ? LIMIT ? ", subscriptionMapper, offset, command.getPageSize());

        return new GenericPage<>(items, command.getPageNum(), command.getPageSize(), count);
    }

    @Override
    public Collection<NotificationTrigger> listTriggers() {
        return getJdbcTemplate().query(SQL_LIST_TRIGGERS, triggerMapper);
    }

    @Override
    public Collection<Subscription> listTriggerSubscriptions(NotificationTrigger trigger) {
        Collection<Subscription> result = getJdbcTemplate().query(SQL_GET_SUBSCRIPTIONS_BY_TRIGGER_ID, subscriptionMapper, trigger.getId());
        if (!result.isEmpty()) {
            for (Subscription subscription : result) {
                subscription.setTriggersKeys(getJdbcTemplate().queryForList(SQL_LIST_TRIGGER_KEYS_BY_SUBSCRIPTION, Long.class, subscription.getId()));
            }
        }
        return result;
    }

    @Override
    public Collection<Long> listTriggerKeys() {
        return getJdbcTemplate().queryForList(SQL_LIST_TRIGGER_KEYS, Long.class);
    }
}
