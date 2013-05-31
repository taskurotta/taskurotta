package ru.hazelcast;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.core.IMap;
import com.hazelcast.spring.mongodb.MongoMapStore;
import com.mongodb.MongoClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.hazelcast.store.MemoryMapStore;
import ru.hazelcast.store.MemoryMapStoreManager;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * User: stukushin
 * Date: 30.05.13
 * Time: 11:17
 */
public class Utils {
    public static Config getConfig() {
        Config config = new ClasspathXmlConfig("hazelcast.xml");

        MapConfig mapConfig = config.getMapConfig("default");

        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setEnabled(true);
        // mapStoreConfig.setWriteDelaySeconds(1);
        setMemoryMapStore(mapStoreConfig);
        // setMongoMapStore(mapStoreConfig);

        mapConfig.setMapStoreConfig(mapStoreConfig);

        return config;
    }

    public static String hzMapToString(IMap<Object, Object> iMap) {
        StringBuilder stringBuilder = new StringBuilder("{");

        for (Map.Entry<Object, Object> entry : iMap.entrySet())  {
            stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }

        if (stringBuilder.length() > 2) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    private static void setMongoMapStore(MapStoreConfig mapStoreConfig) {
        String dbName = "hazelcast-store";
        MongoClient mongoClient = null;
        try {
            mongoClient = new MongoClient("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //mongoClient.dropDatabase(dbName);
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, dbName);

        MongoMapStore mongoMapStore = new MongoMapStore();
        mongoMapStore.setMongoTemplate(mongoTemplate);

        mapStoreConfig.setClassName("com.hazelcast.spring.mongodb.MongoMapStore");
        mapStoreConfig.setImplementation(mongoMapStore);
    }

    private static void setMemoryMapStore(MapStoreConfig mapStoreConfig) {
        MemoryMapStore memoryMapStore = MemoryMapStoreManager.getMemoryMapStore();
        mapStoreConfig.setClassName(String.valueOf(MemoryMapStore.class));
        mapStoreConfig.setImplementation(memoryMapStore);
    }
}