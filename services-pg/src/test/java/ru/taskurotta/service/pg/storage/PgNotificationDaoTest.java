package ru.taskurotta.service.pg.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.Assert;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PgNotificationDaoTest {

    public static ObjectMapper mapper = new ObjectMapper();

    final static List<String> ACTOR_IDS = Arrays.asList("ru.test.Actor1#1.0", "ru.test.Actor2#1.2");
    final static List<String> EMAILS = Arrays.asList("test1@example.com", "test2@example.com");


    JdbcTemplate jdbcTemplate;
    BasicDataSource ds;
    PgNotificationDao target;

    @Before
    public void init() throws IOException {
//        File propsFile = new File(System.getProperty("pg.test.properties.location", "src/test/resources/pg.properties"));
        File propsFile = new File(System.getProperty("pg.test.properties.location", "pg.test.properties"));
        if (propsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                Properties props = new Properties();
                props.load(fis);
                ds = new BasicDataSource();
                ds.setDriverClassName("org.postgresql.Driver");
                ds.setUrl(props.getProperty("pg.db.url"));
                ds.setInitialSize(1);
                ds.setMaxActive(3);
                ds.setUsername(props.getProperty("pg.db.user"));
                ds.setPassword(props.getProperty("pg.db.password"));
                target = new PgNotificationDao();
                target.setDataSource(ds);
                jdbcTemplate = new JdbcTemplate(ds);

                jdbcTemplate.update("delete from TSK_NFN_TRIGGERS");
                jdbcTemplate.update("delete from TSK_NFN_SUBSCRIPTIONS");
                jdbcTemplate.update("delete from TSK_NFN_LINKS");
            }
        }
    }

    @After
    public void close() throws SQLException {
        if (ds != null) {
            ds.close();
        }
    }

    @Test
    public void test() throws IOException {
        if (target != null) {
            NotificationTrigger trigger1 = trigger();
            NotificationTrigger trigger2 = trigger();
            long trigger1Key = target.addTrigger(trigger1);
            long trigger2Key = target.addTrigger(trigger2);
            Assert.assertTrue(trigger1Key > 0);
            Assert.assertTrue(trigger2Key > 0);
            trigger1.setId(trigger1Key);
            trigger2.setId(trigger2Key);

            List<Long> triggerKeys = Arrays.asList(trigger1Key, trigger2Key);

            Collection<Long> triggerIds = target.listTriggerKeys();
            Assert.assertEquals(2, triggerIds.size());

            NotificationTrigger storedTrigger1 = target.getTrigger(trigger1Key);
            compareTriggers(trigger1, storedTrigger1);

            Subscription subscription = subscription(triggerKeys);
            long subscriptionId = target.addSubscription(subscription);
            subscription.setId(subscriptionId);
            Assert.assertTrue(subscriptionId > 0);
            Subscription storedSubscription = target.getSubscription(subscriptionId);
            compareSubscriptions(subscription, storedSubscription);

            Collection<Subscription> triggerSubscriptions = target.listTriggerSubscriptions(trigger1);
            Assert.assertEquals(1, triggerSubscriptions.size());
            compareSubscriptions(subscription, triggerSubscriptions.iterator().next());

            Collection<Subscription> allSubscriptions = target.listSubscriptions();
            Assert.assertEquals(1, allSubscriptions.size());

            SearchCommand command = new SearchCommand();
            command.setPageNum(1);
            command.setPageSize(10);

            for(int i = 0; i<10; i++) {
                target.addSubscription(subscription(triggerKeys));
            }

            GenericPage<Subscription> page = target.listSubscriptions(command);
            Assert.assertEquals(10, page.getItems().size());
            Assert.assertEquals(11, page.getTotalCount());
        }
    }

    void compareSubscriptions(Subscription expected, Subscription actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getChangeDate(), actual.getChangeDate());
        Assert.assertEquals(expected.getActorIds(), actual.getActorIds());
        Assert.assertEquals(expected.getEmails(), actual.getEmails());
        Assert.assertEquals(expected.getTriggersKeys(), actual.getTriggersKeys());
    }

    void compareTriggers(NotificationTrigger expected, NotificationTrigger actual) throws IOException {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getType(), actual.getType());
        compareJsons(expected.getCfg(), actual.getCfg());
        compareJsons(expected.getStoredState(), actual.getStoredState());
        Assert.assertEquals(expected.getChangeDate(), actual.getChangeDate());
    }


    void compareJsons(String expected, String actual) throws IOException {
        JsonNode expectedNode = mapper.readValue(expected, JsonNode.class);
        JsonNode actualNode = mapper.readValue(actual, JsonNode.class);
        Assert.assertEquals(expectedNode, actualNode);
    }

    NotificationTrigger trigger() {
        NotificationTrigger result = new NotificationTrigger();
        result.setCfg("{\"cfgKey\":\"cfgValue\"}");
        result.setStoredState("{\"stateKey\":\"stateValue\"}");
        result.setType("test");
        result.setChangeDate(new Date());
        return result;
    }

    Subscription subscription(List<Long> triggers) {
        Subscription result = new Subscription();
        result.setActorIds(ACTOR_IDS);
        result.setTriggersKeys(triggers);
        result.setEmails(EMAILS);
        result.setChangeDate(new Date());
        return result;
    }


}
