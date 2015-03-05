package ru.taskurotta.test.stress.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.test.stress.ProcessesCounter;

/**
 * Created on 16.02.2015.
 *
 * WARNING! GC should be disabled  for counter to work
 */
public class MongoFpCounter implements ProcessesCounter {

    private MongoTemplate mongoTemplate;

    private String collectionName;

    private void init() {
        DBCollection processCol = mongoTemplate.getCollection(collectionName);
        processCol.createIndex(new BasicDBObject("state", 1));
    }

    @Override
    public long getCount() {
        DBCollection processCol = mongoTemplate.getCollection(collectionName);
        return processCol.count(new BasicDBObject("state", 1));
    }

    @Required
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Required
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

}
