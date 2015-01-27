package ru.taskurotta.hazelcast.queue.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.ServiceConfig;
import ru.taskurotta.hazelcast.queue.impl.QueueDataSerializerHook;
import ru.taskurotta.hazelcast.queue.impl.QueueService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hazelcast.partition.strategy.StringPartitioningStrategy.getBaseName;

/**
 */
public class CachedQueueServiceConfig extends ServiceConfig {

    private final Map<String, CachedQueueConfig> queueConfigs = new ConcurrentHashMap<String, CachedQueueConfig>();

    /**
     * WARNING: not thread safe!
     *
     * @param config
     * @param name
     * @return
     */
    public static CachedQueueConfig getQueueConfig(Config config, String name) {
        CachedQueueServiceConfig queueServiceConfig = registerServiceConfig(config);

        return queueServiceConfig.getQueueConfig(name);
    }

    /**
     * WARNING: not thread safe!
     *
     * @param config
     * @return
     */
    public static CachedQueueServiceConfig registerServiceConfig(Config config) {
        CachedQueueServiceConfig queueServiceConfig = (CachedQueueServiceConfig) config.getServicesConfig()
                .getServiceConfig(QueueService.SERVICE_NAME);

        if (queueServiceConfig == null) {
            queueServiceConfig = new CachedQueueServiceConfig();
            queueServiceConfig.setEnabled(true);
            queueServiceConfig.setName(QueueService.SERVICE_NAME);
            queueServiceConfig.setClassName(QueueService.class.getName());
            config.getServicesConfig().addServiceConfig(queueServiceConfig);
            config.getSerializationConfig().addDataSerializableFactory(QueueDataSerializerHook.F_ID, new
                    QueueDataSerializerHook().createFactory());
        }

        return queueServiceConfig;
    }

    public CachedQueueConfig getQueueConfig(String name) {
        String baseName = getBaseName(name);
        CachedQueueConfig config = lookupByPattern(queueConfigs, baseName);
        if (config != null) {
            return config;
        }
        CachedQueueConfig defConfig = queueConfigs.get("default");
        if (defConfig == null) {
            defConfig = new CachedQueueConfig();
            defConfig.setName("default");
            addQueueConfig(defConfig);
        }
        config = new CachedQueueConfig(defConfig);
        config.setName(name);
        addQueueConfig(config);
        return config;
    }

    public CachedQueueServiceConfig addQueueConfig(CachedQueueConfig queueConfig) {
        queueConfigs.put(queueConfig.getName(), queueConfig);
        return this;
    }

    public Map<String, CachedQueueConfig> getQueueConfigs() {
        return queueConfigs;
    }

    public CachedQueueServiceConfig setQueueConfigs(Map<String, CachedQueueConfig> queueConfigs) {
        this.queueConfigs.clear();
        this.queueConfigs.putAll(queueConfigs);
        for (Map.Entry<String, CachedQueueConfig> entry : queueConfigs.entrySet()) {
            entry.getValue().setName(entry.getKey());
        }
        return this;
    }

    public CachedQueueConfig findQueueConfig(String name) {
        String baseName = getBaseName(name);
        CachedQueueConfig config = lookupByPattern(queueConfigs, baseName);
        if (config != null) {
            return config;
        }
        return getQueueConfig("default");
    }


    private static <T> T lookupByPattern(Map<String, T> map, String name) {
        T t = map.get(name);
        if (t == null) {
            for (Map.Entry<String, T> entry : map.entrySet()) {
                String pattern = entry.getKey();
                T value = entry.getValue();
                if (nameMatches(name, pattern)) {
                    return value;
                }
            }
        }
        return t;
    }

    public static boolean nameMatches(final String name, final String pattern) {
        final int index = pattern.indexOf('*');
        if (index == -1) {
            return name.equals(pattern);
        } else {
            final String firstPart = pattern.substring(0, index);
            final int indexFirstPart = name.indexOf(firstPart, 0);
            if (indexFirstPart == -1) {
                return false;
            }
            final String secondPart = pattern.substring(index + 1);
            final int indexSecondPart = name.indexOf(secondPart, index + 1);
            return indexSecondPart != -1;
        }
    }

}
