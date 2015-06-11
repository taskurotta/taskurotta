package ru.taskurotta.service.notification.dao.impl;

import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.taskurotta.service.notification.model.NotificationTrigger;

import java.io.File;
import java.util.Date;

/**
 * Created on 11.06.2015.
 */
public class JsonNotificationDaoTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testTriggerCreate() throws Exception {
        File tmpDir = tmp.newFolder();
        JsonNotificationDao target = new JsonNotificationDao(tmpDir.getPath());
        NotificationTrigger trigger1 = createTrigger();
        NotificationTrigger trigger2 = createTrigger();

        long id1 = target.addTrigger(trigger1);
        long id2 = target.addTrigger(trigger2);

        Assert.assertTrue(new File(tmpDir, JsonNotificationDao.DIR_TRIGGERS).exists());

        Assert.assertNotNull(target.getTrigger(id1));
        Assert.assertNotNull(target.getTrigger(id2));
    }

    private NotificationTrigger createTrigger() {
        NotificationTrigger result = new NotificationTrigger();
        result.setType("test");
        result.setChangeDate(new Date());
        result.setCfg("{\"key\": \"value\"}");
        result.setStoredState("[]");
        return result;
    }


}
