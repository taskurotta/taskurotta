package ru.taskurotta.spring.configs;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:52
 */
public class SpreaderConfigPathXmlApplicationContextTest {

    private SpreaderConfigPathXmlApplicationContext spreaderConfigPathXmlApplicationContext;

    @Before
    public void setUp() throws Exception {
        String pathToXmlContext = "/SpreaderBeans.xml";

        spreaderConfigPathXmlApplicationContext = new SpreaderConfigPathXmlApplicationContext();
        spreaderConfigPathXmlApplicationContext.setContext(pathToXmlContext);
        spreaderConfigPathXmlApplicationContext.init();
    }

    @Test
    public void testGetTaskSpreader() throws Exception {
        assertNotNull(spreaderConfigPathXmlApplicationContext.getTaskSpreader(TestActor.class));
    }
}
