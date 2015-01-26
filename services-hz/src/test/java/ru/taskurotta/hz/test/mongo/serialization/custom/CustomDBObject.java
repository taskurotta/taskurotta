package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.DBObject;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import ru.taskurotta.transport.model.TaskContainer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 26/01/15.
 */
public class CustomDBObject implements DBObject {

    private ObjectId objectId;
    private TaskContainer taskContainer;
    private Set<String> hashSet = new HashSet<>();
    private boolean _isPartialObject;


    public CustomDBObject() {
        hashSet.add("_id");
        hashSet.add("taskContainer");
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    public TaskContainer getTaskContainer() {
        return taskContainer;
    }

    public void setTaskContainer(TaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    @Override
    public Object put(String key, Object v) {
        switch (key) {
            case "taskContainer":
                taskContainer = (TaskContainer) v;
                return taskContainer;
            case "_id":
                objectId = (ObjectId) v;
                return objectId;
        }
        return null;
    }

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
    public Object get(String key) {
        switch (key) {
            case "taskContainer":
                return taskContainer;
            case "_id":
                return objectId;
        }
        return null;
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
        return hashSet.contains(s);
    }

    @Override
    public Set<String> keySet() {
        return hashSet;
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
