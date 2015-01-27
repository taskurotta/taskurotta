package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.DBObject;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 26/01/15.
 */
public abstract class CustomDBObject<T> implements DBObject {

    protected ObjectId objectId;
    protected T object;
    protected Set<String> keySet = new HashSet<>();
    private boolean _isPartialObject;


    public CustomDBObject() {
        initKeySet();
    }

    protected abstract void initKeySet();

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }


    public T getObject() {
        if (object == null) {
            object = createObject();
        }
        return object;
    }


    public void setObject(T object) {
        this.object = object;
    }

    protected abstract T createObject();

    @Override
    public void putAll(BSONObject o) {
        for (String key : o.keySet()) {
            put(key, o.get(key));
        }
    }

    @Override
    public void putAll(Map m) {
        throw new NotImplementedException();
    }



    @Override
    public Map toMap() {
        throw new NotImplementedException();
    }

    @Override
    public Object removeField(String key) {
        throw new NotImplementedException();
    }

    @Override
    public boolean containsKey(String s) {
        throw new NotImplementedException();
    }

    @Override
    public boolean containsField(String s) {
        return keySet.contains(s);
    }

    @Override
    public Set<String> keySet() {
        return keySet;
    }

    @Override
    public boolean isPartialObject() {
        return _isPartialObject;
    }

    @Override
    public void markAsPartialObject() {
        _isPartialObject = true;
    }
}
