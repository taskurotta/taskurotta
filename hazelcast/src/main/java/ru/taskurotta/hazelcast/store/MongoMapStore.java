package ru.taskurotta.hazelcast.store;
/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapLoaderLifecycleSupport;
import com.hazelcast.core.MapStore;
import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class MongoMapStore implements MapStore, MapLoaderLifecycleSupport {

    private final static Logger logger = LoggerFactory.getLogger(MongoMapStore.class);

    public static Timer storeTimer = Metrics.newTimer(MongoMapStore.class, "store",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer loadTimer = Metrics.newTimer(MongoMapStore.class, "load",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer loadSuccessTimer = Metrics.newTimer(MongoMapStore.class, "load_success",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    public static Timer deleteTimer = Metrics.newTimer(MongoMapStore.class, "delete",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

    private static WriteConcern noWaitWriteConcern = new WriteConcern(0, 0, false, true);

    private String mapName;
    private MongoDBConverter converter;
    private DBCollection coll;
    private MongoTemplate mongoTemplate;

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void store(Object key, Object value) {
        long startTime = System.nanoTime();

        try {
            DBObject dbo = converter.toDBObject(value);
            dbo.put("_id", key);
            coll.save(dbo);
        } finally {
            storeTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    public void storeAll(Map map) {
        for (Object key : map.keySet()) {
            store(key, map.get(key));
        }
    }

    public void delete(Object key) {
        long startTime = System.nanoTime();

        try {
            DBObject dbo = new BasicDBObject();
            dbo.put("_id", key);
            coll.remove(dbo, noWaitWriteConcern);
        } finally {
            deleteTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    public void deleteAll(Collection keys) {

        int j = 0;
        int size = keys.size();

        BasicDBList idList = new BasicDBList();

        for (Object id: keys) {
            j ++;

            idList.add(id);

            if (j % 100 == 0 && j == size) {
                BasicDBObject inListObj = new BasicDBObject("$in", idList);
                coll.remove(new BasicDBObject("_id", inListObj), noWaitWriteConcern);
                idList.clear();;
            }
        }
    }


    public Object load(Object key) {
        long startTime = System.nanoTime();

        try {

            DBObject dbo = new BasicDBObject();
            dbo.put("_id", key);
            DBObject obj = coll.findOne(dbo);
            if (obj == null)
                return null;

            try {
                Class clazz = Class.forName(obj.get("_class").toString());
                Object result = converter.toObject(clazz, obj);

                if (result != null) {
                    loadSuccessTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                }

                return result;
            } catch (ClassNotFoundException e) {
                logger.warn(e.getMessage(), e);
            }
            return null;

        } finally {
            loadTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    public Map loadAll(Collection keys) {

        // ignore bulk load

        return new HashMap();
    }

    public Set loadAllKeys() {

        // ignore bulk load

        return new HashSet();
    }

    public void init(HazelcastInstance hazelcastInstance, Properties properties, String mapName) {
        if (properties.get("collection") != null) {
            this.mapName = (String) properties.get("collection");
        } else {
            this.mapName = mapName;
        }

        this.coll = mongoTemplate.getCollection(this.mapName);
        this.converter = new SpringMongoDBConverter(mongoTemplate);

        logger.debug("Store for collection [" + mapName + "] initialized");

    }

    public void destroy() {
    }
}
