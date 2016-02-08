package ru.taskurotta.test.stress.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.test.stress.ProcessesCounter;

/**
 * Created on 17.02.2015.
 */
public class MongoBrokenProcessCounter implements ProcessesCounter {
    private DB mongoDB;
    private String collectionName;

    private void init() {
        DBCollection processCol = mongoDB.getCollection(collectionName);
        processCol.createIndex(new BasicDBObject("state", 1));
    }

    @Override
    public int getCount() {
        DBCollection processCol = mongoDB.getCollection(collectionName);
        return (int) processCol.count(new BasicDBObject("state", 2));
    }

    @Required
    public void setMongoDB(DB mongoDB) {
        this.mongoDB = mongoDB;
    }

    @Required
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
