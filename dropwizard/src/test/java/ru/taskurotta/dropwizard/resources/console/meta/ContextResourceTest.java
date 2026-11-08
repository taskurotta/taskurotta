package ru.taskurotta.dropwizard.resources.console.meta;

import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * REST-ресурс /console/context: чувствительные свойства (креденшалы БД,
 * legacy-учётки, mail-учётка) не попадают в веб-ответ открытым текстом —
 * значение заменяется на *** (точный список ключей + страховочные подстроки),
 * остальные свойства проходят без изменений.
 */
public class ContextResourceTest {

    private static final String MASKED = "***";

    private Map<String, String> propsWith(Properties extra) {
        ContextResource resource = new ContextResource();
        Properties properties = new Properties();
        properties.putAll(extra);
        resource.setProperties(properties);
        return resource.getTaskServerProperties();
    }

    @Test
    public void exactSensitiveKeysAreMasked() {
        Properties p = new Properties();
        p.setProperty("password", "db-pass");
        p.setProperty("url", "jdbc:postgresql://host:5432/db");
        p.setProperty("user", "db-user");
        p.setProperty("oradb.password", "legacy-pass");
        p.setProperty("oradb.user", "legacy-user");
        p.setProperty("notifications.mail.password", "mail-pass");
        p.setProperty("notifications.mail.user", "mail-user");
        p.setProperty("notifications.mail.from", "from@host");

        Map<String, String> result = propsWith(p);

        for (String key : p.stringPropertyNames()) {
            assertEquals(MASKED, result.get(key));
        }
    }

    @Test
    public void sensitiveSubstringsAreMaskedCaseInsensitively() {
        Properties p = new Properties();
        p.setProperty("db.password", "value-1");
        p.setProperty("my.pwd", "value-2");
        p.setProperty("api.SECRET", "value-3");
        p.setProperty("auth.TOKEN", "value-4");
        p.setProperty("custom.PasswOrd", "value-5");

        Map<String, String> result = propsWith(p);

        for (String key : p.stringPropertyNames()) {
            assertEquals(MASKED, result.get(key));
        }
    }

    @Test
    public void otherPropertiesPassThroughUnchanged() {
        Properties p = new Properties();
        p.setProperty("hz.threads", "16");
        p.setProperty("mongo.options.timeout", "5000");
        p.setProperty("recovery.enabled", "true");

        Map<String, String> result = propsWith(p);

        assertEquals("16", result.get("hz.threads"));
        assertEquals("5000", result.get("mongo.options.timeout"));
        assertEquals("true", result.get("recovery.enabled"));
    }

    @Test
    public void emptyPropertiesReturnNull() {
        assertNull(propsWith(new Properties()));

        ContextResource resource = new ContextResource();
        assertNull(resource.getTaskServerProperties());
    }
}
