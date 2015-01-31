package ru.taskurotta.spring.configs;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:44
 */
public class RuntimeConfigPathXmlApplicationContextTest {

    private RuntimeConfigPathXmlApplicationContext runtimeConfigPathXmlApplicationContext;

    @Before
    public void setUp() throws Exception {
        String pathToXmlContext = "/RuntimeBeans.xml";

        Properties properties = new Properties();
        properties.setProperty("replacedValue", "replacedValue");

        runtimeConfigPathXmlApplicationContext = new RuntimeConfigPathXmlApplicationContext();
        runtimeConfigPathXmlApplicationContext.setContext(pathToXmlContext);
        runtimeConfigPathXmlApplicationContext.setProperties(properties);
        runtimeConfigPathXmlApplicationContext.setDefaultPropertiesLocations(new String[]{"default1.properties", "default2.properties"});
        runtimeConfigPathXmlApplicationContext.init();
    }

    @Test
    public void testGetRuntimeProcessor() throws Exception {
        assertNotNull(runtimeConfigPathXmlApplicationContext.getRuntimeProcessor(TestActor.class));

        Field field = runtimeConfigPathXmlApplicationContext.getClass().getSuperclass().getDeclaredField("applicationContext");
        field.setAccessible(true);
        AbstractApplicationContext applicationContext = (AbstractApplicationContext) field.get(runtimeConfigPathXmlApplicationContext);

        TestActorImpl testActor = applicationContext.getBean(TestActorImpl.class);
        assertEquals("defaultValue1", testActor.getDefaultValue1());
        assertEquals("defaultValue2", testActor.getDefaultValue2());
        assertEquals("replacedValue", testActor.getReplacedValue());
    }
}
