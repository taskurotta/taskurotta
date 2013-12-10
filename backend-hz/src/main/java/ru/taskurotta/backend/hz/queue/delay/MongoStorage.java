package ru.taskurotta.backend.hz.queue.delay;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:35 AM
 */
public class MongoStorage implements Storage {

    private DBCollection dbCollection;

    public static final String OBJECT_NAME = "object";
    public static final String ENQUEUE_TIME_NAME = "enqueueTime";

    public MongoStorage(DBCollection dbCollection) {
        this.dbCollection = dbCollection;
    }

    @Override
    public boolean add(Object o, long delayTime, TimeUnit unit) {
        long enqueueTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delayTime, unit);

        DBObject dbObject = new BasicDBObject().append(OBJECT_NAME, o).append(ENQUEUE_TIME_NAME, enqueueTime);

        WriteResult writeResult = dbCollection.save(dbObject);

        return writeResult.getError() == null;
    }

    @Override
    public boolean remove(Object o) {
        BasicDBObject query = new BasicDBObject(MongoStorage.OBJECT_NAME, new BasicDBObject("$in", o));

        try (DBCursor dbCursor = dbCollection.find(query)) {

            if (dbCursor.size() == 0) {
                return false;
            }

            WriteResult writeResult = dbCollection.remove(dbCursor.next());
            return writeResult.getError() != null;
        }
    }

    @Override
    public boolean contains(Object o) {
        BasicDBObject query = new BasicDBObject(MongoStorage.OBJECT_NAME, new BasicDBObject("$in", o));

        try (DBCursor dbCursor = dbCollection.find(query)) {
            return dbCursor.size() != 0;
        }
    }

    @Override
    public void clear() {
        dbCollection.drop();
    }

    @Override
    public void destroy() {
        dbCollection.drop();
    }
}
