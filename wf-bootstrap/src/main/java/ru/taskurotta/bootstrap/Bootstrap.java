package ru.taskurotta.bootstrap;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.config.ActorConfig;
import ru.taskurotta.bootstrap.config.Config;
import ru.taskurotta.bootstrap.config.ProfilerConfig;
import ru.taskurotta.bootstrap.config.RuntimeConfig;
import ru.taskurotta.bootstrap.config.SpreaderConfig;
import ru.taskurotta.bootstrap.profiler.MetricsProfiler;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 14:53
 */
public abstract class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    protected void run(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("prog");
        parser.addArgument("-f", "--file")
                .required(false)
                .help("Specify config file to use");

        parser.addArgument("-r", "--resource")
                .required(false)
                .help("Specify resource file (in classpath) to use");

        Namespace namespace = parser.parseArgs(args);

        Config config = null;

        String configFileName = namespace.getString("file");

        if (configFileName != null) {

            File configFile = new File(configFileName);
            if (configFile.exists()) {

                config = Config.valueOf(configFile);
            } else {

                System.out.println("File doesn't exist: " + configFileName);
                parser.printHelp();
                return;
            }

        }

        String resourceFileName = namespace.getString("resource");

        if (resourceFileName != null) {

            URL configPath = Thread.currentThread().getContextClassLoader().getResource(resourceFileName);
            logger.debug("Config file URL is [{}]", configPath);

            if (configPath != null) {
                config = Config.valueOf(configPath);
            } else {

                System.out.println("Resource file (in classpath) doesn't exist: " + configFileName);
                parser.printHelp();
                return;
            }
        }

        if (config == null) {
            System.out.println("Config file doesn't specified");
            parser.printHelp();
            return;
        }


        for (ActorConfig actorConfig : config.actorConfigs) {

            Class actorClass;

            try {
                actorClass = Class.forName(actorConfig.getActorInterface());
            } catch (ClassNotFoundException e) {
                logger.error("Not found class [{}]", actorConfig.getActorInterface());
                throw new RuntimeException("Not found class " + actorConfig.getActorInterface(), e);
            }

            SpreaderConfig taskSpreaderConfig = config.spreaderConfigs.get(actorConfig.getSpreaderConfig());
            TaskSpreader taskSpreader = taskSpreaderConfig.getTaskSpreader(actorClass);

            RuntimeConfig runtimeConfig = config.runtimeConfigs.get(actorConfig.getRuntimeConfig());
            RuntimeProcessor runtimeProcessor = runtimeConfig.getRuntimeProcessor(actorClass);

            ProfilerConfig profilerConfig = config.profilerConfigs.get(actorConfig.getProfilerConfig());
            Profiler profiler = (profilerConfig == null) ? new SimpleProfiler(actorClass) : profilerConfig.getProfiler(actorClass);

            ActorExecutor actorExecutor = new ActorExecutor(profiler, runtimeProcessor, taskSpreader);

            int count = actorConfig.getCount();
            ExecutorService executorService = Executors.newFixedThreadPool(count);

            for (int i = 0; i < count; i++) {
                executorService.execute(actorExecutor);
            }
        }
    }

}
