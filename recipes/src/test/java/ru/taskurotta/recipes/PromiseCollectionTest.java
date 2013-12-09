package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.recipes.pcollection.PromiseCollectionArbiter;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static ru.taskurotta.recipes.RecipesRunner.run;

/**
 * Test for processing Promise of a collection or an array
 * User: dimadin
 * Date: 22.07.13 17:02
 */
public class PromiseCollectionTest {
    private static final Logger logger = LoggerFactory.getLogger(PromiseCollectionTest.class);

    @Test
    public void start() throws ArgumentParserException, IOException, ClassNotFoundException {
        run("ru/taskurotta/recipes/pcollection/");
        PromiseCollectionArbiter arbiter = PromiseCollectionArbiter.getInstance();
        logger.info("waiting for arbiter [{}]", arbiter.uuid);
        assertTrue(arbiter.waitForFinish(5000));
    }

}
