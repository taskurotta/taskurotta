package ru.taskurotta.hz.test;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.hz.test.bson.MongoSerializationTest;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.service.hz.TaskFatKey;
import ru.taskurotta.service.hz.serialization.bson.TaskContainerSerializer;
import ru.taskurotta.transport.model.TaskContainer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 06/02/15.
 */
public class MongoMapStoreTest {


    private MongoTemplate getMongoTemplateForDb(String db) throws UnknownHostException {
        ServerAddress serverAddress = new ServerAddress("127.0.0.1", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress);

        WriteConcern writeConcern = new WriteConcern(1, 0, false, true);

        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, db);
        mongoTemplate.setWriteConcern(writeConcern);
        return mongoTemplate;
    }


    @Test
    public void test() throws UnknownHostException {

        BSerializationService serializationService = BSerializationServiceFactory.newInstance(new TaskContainerSerializer());
        Config cfg = new Config();
        MulticastConfig multicastConfig = new MulticastConfig();
        multicastConfig.setEnabled(false);
        JoinConfig joinConfig = new JoinConfig();
        joinConfig.setMulticastConfig(multicastConfig);
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setJoin(joinConfig);
        cfg.setNetworkConfig(networkConfig);
        MongoMapStore mongoMapStore = new MongoMapStore(serializationService, TaskContainer.class);
        mongoMapStore.setMongoTemplate(getMongoTemplateForDb("test-mongo"));
        MapConfig mapConfig = new MapConfig();
        mapConfig.setName("tasks");
        MapStoreConfig mapStoreConfig = new MapStoreConfig();
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setImplementation(mongoMapStore);
        mapStoreConfig.setWriteDelaySeconds(0);
        mapConfig.setMapStoreConfig(mapStoreConfig);
        cfg.addMapConfig(mapConfig);
        cfg.setProperty("backup-count", "0");
        cfg.setProperty("time-to-live-seconds", "0");
        cfg.setProperty("eviction-policy", "LRU");
        cfg.setProperty("max-size", "1000");
        cfg.setProperty("eviction-percentage", "25");
        cfg.setProperty("max-size-policy", "USED_HEAP_PERCENTAGE");
        cfg.setProperty("max-idle-seconds", "0");


        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);

        IMap<TaskFatKey, TaskContainer> map = hazelcastInstance.getMap("tasks");
        List<TaskFatKey> keys = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            TaskContainer taskContainer = MongoSerializationTest.createTaskContainer();
            TaskFatKey taskFatKey = new TaskFatKey(taskContainer.getProcessId(), taskContainer.getTaskId());
            map.put(taskFatKey, taskContainer);
            keys.add(taskFatKey);
        }
        System.out.println("map = " + map.size());

        for (TaskFatKey key : keys) {
            TaskContainer taskContainer = map.get(key);
            Assert.assertEquals(true, taskContainer.getTaskId().equals(key.getTaskId()));
            Assert.assertEquals(true, taskContainer.getProcessId().equals(key.getProcessId()));

        }
    }

}
