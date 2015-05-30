package ru.taskurotta.test.stress.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.test.stress.ProcessesCounter;

/**
 * Created on 16.02.2015.
 *
 * WARNING! GC should be disabled  for counter to work
 */
public class MongoFpCounter implements ProcessesCounter {

    private DB mongoDB;
    private String collectionName;
    private String findField;
    private String findQuery;

    @Override
    public long getCount() {
        DBCollection processCollection = mongoDB.getCollection(collectionName);
        return processCollection.count(new BasicDBObject(findField, findQuery));
    }

    @Required
    public void setMongoDB(DB mongoDB) {
        this.mongoDB = mongoDB;
    }

    @Required
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @Required
    public void setFindField(String findField) {
        this.findField = findField;
    }

    @Required
    public void setFindQuery(String findQuery) {
        this.findQuery = findQuery;
    }
}
