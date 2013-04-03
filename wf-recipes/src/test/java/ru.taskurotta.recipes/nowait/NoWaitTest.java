package ru.taskurotta.recipes.nowait;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.bootstrap.config.Config;
import ru.taskurotta.spring.configs.RuntimeConfigPathXmlApplicationContext;
import ru.taskurotta.test.BasicFlowArbiter;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by void 02.04.13 13:22
 */
public class NoWaitTest {

	//@org.junit.Ignore
	@Test
	public void start() throws ArgumentParserException, IOException, ClassNotFoundException {
		Bootstrap bootstrap = new Bootstrap();
		Config config = bootstrap.parseArgs(new String[]{"-r", "ru/taskurotta/recipes/nowait/wf-config.yml"});
		RuntimeConfigPathXmlApplicationContext mainRuntimeConfig = (RuntimeConfigPathXmlApplicationContext)config.runtimeConfigs.get("MainRuntimeConfig");
		BasicFlowArbiter arbiter = mainRuntimeConfig.getApplicationContext().getBean("arbiter", BasicFlowArbiter.class);
		bootstrap.start(config);

		assertTrue(arbiter.waitForFinish(20000));

	}
}
