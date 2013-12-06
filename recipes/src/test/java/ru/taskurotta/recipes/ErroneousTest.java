package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.Bootstrap;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by void 18.10.13 19:09
 */
public class ErroneousTest {
    protected final static Logger log = LoggerFactory.getLogger(ErroneousTest.class);

    @Test
    public void start() throws ArgumentParserException, IOException, ClassNotFoundException, InterruptedException {
        new Bootstrap("ru/taskurotta/recipes/erroneous/conf.yml").start();

        TimeUnit.SECONDS.sleep(3); // ToDo: need arbiter here

        log.info("test finished");
    }

}
