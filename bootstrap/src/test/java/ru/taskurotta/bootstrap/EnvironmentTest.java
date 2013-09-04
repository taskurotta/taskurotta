package ru.taskurotta.bootstrap;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.Environment;

/**
 * User: greg
 */
public class EnvironmentTest {

    @Test
    public void testEnviroment(){
        Environment environment = Environment.getInstance();
        Assert.assertEquals(Environment.Type.TEST, environment.getType());
    }
}
