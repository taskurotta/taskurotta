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
import ru.taskurotta.bootstrap.config.RetryPolicyConfig;
import ru.taskurotta.bootstrap.config.RuntimeConfig;
import ru.taskurotta.bootstrap.config.SpreaderConfig;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.policy.retry.BlankRetryPolicy;
import ru.taskurotta.policy.retry.RetryPolicy;
import ru.taskurotta.util.ActorDefinition;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 14:53
 */
public class Bootstrap implements BootstrapMBean {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

	private Config config;
    private Map<String, Thread> shutdownHookThreadMap = new HashMap<String, Thread>();
    private Map<String, ActorConfig> actorConfigMap = new HashMap<String, ActorConfig>();
    private Map<String, ActorThreadPool> actorThreadPoolMap = new HashMap<String, ActorThreadPool>();

    public Bootstrap(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
		config = parseArgs(args);
	}

	public Bootstrap(String configResourceName) throws ArgumentParserException, IOException, ClassNotFoundException {
		config = parseArgs(new String[]{"-r", configResourceName});
	}

	public Config parseArgs(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
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
                logger.error("Configuration file doesn't exist: [{}]", configFileName);
                parser.printHelp();
                return null;
            }

        }

        String resourceFileName = namespace.getString("resource");

        if (resourceFileName != null) {

            URL configPath = Thread.currentThread().getContextClassLoader().getResource(resourceFileName);

            if (configPath != null) {
                config = Config.valueOf(configPath);
            } else {
                logger.error("Resource file (in classpath) doesn't exist: [{}]", resourceFileName);
                parser.printHelp();
                return null;
            }
        }

        if (config == null) {
            System.out.println("Config file doesn't specified");
            parser.printHelp();
            return null;
        }

		return config;
	}

	public void start() {
		start(config);
	}

    @Override
    public Map<String, Integer> getActorPoolSizes() {
        Map<String, Integer> actorPoolSizes = new HashMap<String, Integer>();

        Set<String> actorPoolIdSet = actorThreadPoolMap.keySet();

        for (String actorPoolId : actorPoolIdSet) {
            actorPoolSizes.put(actorPoolId, actorThreadPoolMap.get(actorPoolId).getCurrentSize());
        }

        return actorPoolSizes;
    }

    @Override
    public synchronized void startActorPool(String actorId, int poolSize) {
        ActorConfig actorConfig = actorConfigMap.get(actorId);

        if (actorConfig == null) {
            logger.error("Not found actorConfig for actorId [{}]", actorId);
            return;
        }

        final Class actorClass = getActorClass(actorConfig.getActorInterface());

        SpreaderConfig taskSpreaderConfig = config.spreaderConfigs.get(actorConfig.getSpreaderConfig());
        TaskSpreader taskSpreader;
        if (actorConfig.getTaskList() == null) {
            taskSpreader = taskSpreaderConfig.getTaskSpreader(actorClass);
        } else {
            taskSpreader = taskSpreaderConfig.getTaskSpreader(actorClass, actorConfig.getTaskList());
        }

        RuntimeConfig runtimeConfig = config.runtimeConfigs.get(actorConfig.getRuntimeConfig());
        RuntimeProcessor runtimeProcessor = runtimeConfig.getRuntimeProcessor(actorClass);

        ProfilerConfig profilerConfig = config.profilerConfigs.get(actorConfig.getProfilerConfig());
        Profiler profiler = (profilerConfig == null) ? new SimpleProfiler() : profilerConfig.getProfiler(actorClass);

        RetryPolicyConfig retryPolicyConfig = config.policyConfigs.get(actorConfig.getPolicyConfig());
        RetryPolicy retryPolicy = (retryPolicyConfig == null) ? new BlankRetryPolicy() : retryPolicyConfig.getRetryPolicy();

        if (poolSize < 1) {
            poolSize = actorConfig.getCount();
        }

        final ActorThreadPool actorThreadPool = new ActorThreadPool(actorClass, poolSize, actorConfig.getSleepTimeoutMillis(), actorConfig.getShutdownTimeoutMillis());
        final String actorPoolId = saveActorPool(actorId, actorThreadPool);
        Inspector inspector = new Inspector(retryPolicy, actorThreadPool);

        String actorFailoverTime = (String) actorConfig.getProperty(Inspector.FAILOVER_PROPERTY);
        if (actorFailoverTime != null) {
            inspector.setFailover(actorFailoverTime);
        }

        ActorExecutor actorExecutor = new ActorExecutor(profiler, inspector, runtimeProcessor, taskSpreader);
        actorThreadPool.start(actorExecutor);

        Thread thread = new Thread(actorClass.getSimpleName() + " shutdowner") {
            @Override
            public void run() {
                logger.debug("Invoke shutdown hook for actor [{}] pool [{}]", actorClass.getName(), actorPoolId);

                actorThreadPool.shutdown();
            }
        };

        shutdownHookThreadMap.put(actorPoolId, thread);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    @Override
    public synchronized void stopActorPool(String actorPoolId) {
        Thread thread = shutdownHookThreadMap.get(actorPoolId);

        if (thread == null) {
            logger.error("Not found shutdown hook thread for actorPoolId [{}]", actorPoolId);
            return;
        }

        thread.start();
        shutdownHookThreadMap.remove(actorPoolId);
        actorThreadPoolMap.remove(actorPoolId);
    }

    @Override
    public void shutdown() {
        Collection<Thread> threads = shutdownHookThreadMap.values();

        if (threads.isEmpty()) {
            logger.error("Not found shutdown hook threads");
            return;
        }

        for (Thread thread : threads) {
            thread.start();
        }
    }

    public void start(Config config) {
		for (ActorConfig actorConfig : config.actorConfigs) {
            Class actorClass = getActorClass(actorConfig.getActorInterface());

            ActorDefinition actorDefinition = ActorDefinition.valueOf(actorClass);
            String actorId = actorDefinition.getFullName();

            actorConfigMap.put(actorId, actorConfig);

            startActorPool(actorId, actorConfig.getCount());
		}
	}

    private Class getActorClass(String actorInterfaceName) {
        try {
            return Class.forName(actorInterfaceName);
        } catch (ClassNotFoundException e) {
            logger.error("Not found class [{}]", actorInterfaceName);
            throw new RuntimeException("Not found class " + actorInterfaceName, e);
        }
    }

    private String saveActorPool(String actorId, ActorThreadPool actorThreadPool) {
        Set<String> actorPoolIdSet = actorThreadPoolMap.keySet();

        int number = 0;
        for (String actorPoolId : actorPoolIdSet) {
            if (actorPoolId.contains(actorId)) {
                number++;
            }
        }

        String newActorPoolId = createActorPoolId(actorId, number);

        actorThreadPoolMap.put(newActorPoolId, actorThreadPool);

        return newActorPoolId;
    }

    private String createActorPoolId(String actorId, int number) {
        return actorId + "[" + number + "]";
    }
}
