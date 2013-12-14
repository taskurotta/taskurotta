package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.recipes.darg.DArgArbiter;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static ru.taskurotta.recipes.RecipesRunner.run;

/**
 * User: dimadin
 * Date: 16.07.13 14:41
 */
public class DArgTest {

    private static final Logger logger = LoggerFactory.getLogger(DArgTest.class);

    // Ignore it until repair
    @Ignore
    @Test
    public void start() throws ArgumentParserException, IOException, ClassNotFoundException {
        run("ru/taskurotta/recipes/darg/");
        DArgArbiter arbiter = DArgArbiter.getInstance();
        logger.info("waiting for arbiter [{}]", arbiter.uuid);
        assertTrue(arbiter.waitForFinish(5000));
    }

}
