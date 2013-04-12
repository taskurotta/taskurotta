package ru.taskurotta.recipes.nowait;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.test.BasicFlowArbiter;
import ru.taskurotta.test.FlowArbiterFactory;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by void 02.04.13 13:22
 */
public class NoWaitTest {

	//@org.junit.Ignore
	@Test
	public void start() throws ArgumentParserException, IOException, ClassNotFoundException {
		new Bootstrap("ru/taskurotta/recipes/nowait/wf-config.yml").start();

		BasicFlowArbiter arbiter = (BasicFlowArbiter) new FlowArbiterFactory().getInstance(); // created in spring context
		assertTrue(arbiter.waitForFinish(10000));
	}
}
