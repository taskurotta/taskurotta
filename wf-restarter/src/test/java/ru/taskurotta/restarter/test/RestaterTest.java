package ru.taskurotta.restarter.test;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 09.08.13
 * Time: 15:24
 */
public class RestaterTest {

    @Test
    public void start() throws ArgumentParserException, IOException, ClassNotFoundException, InterruptedException {
        new Bootstrap("ru/taskurotta/restarter/test/wf-config.yml").start();

        TimeUnit.SECONDS.sleep(5);
    }
}
