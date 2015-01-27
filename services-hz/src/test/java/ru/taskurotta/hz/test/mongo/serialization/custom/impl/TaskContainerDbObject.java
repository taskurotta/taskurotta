package ru.taskurotta.hz.test.mongo.serialization.custom.impl;

import org.bson.types.ObjectId;
import ru.taskurotta.hz.test.mongo.serialization.custom.CustomDBObject;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * Created by greg on 27/01/15.
 */
public class TaskContainerDbObject extends CustomDBObject<TaskContainer> {


    @Override
    public Object put(String key, Object v) {
        switch (key) {
            case "_id":
                objectId = (ObjectId) v;
                return v;
            case "taskId":
                getObject().setTaskId(UUID.fromString((String) v));
            case "method":
                getObject().setMethod((String) v);
                return v;
            case "actorId":
                getObject().setActorId((String) v);
                return v;
            case "args":
                System.out.println("v = " + v);
//                getObject().setArgs(ArgContainerDbObject.fromArray((ArgContainerDbObject[]) v));
                return v;
        }
        return null;
    }

    @Override
    public Object get(String key) {
        switch (key) {
            case "_id":
                return objectId;
            case "taskId":
                return getObject().getTaskId().toString();
            case "method":
                return getObject().getMethod();
            case "actorId":
                return getObject().getActorId();
            case "args":
                return ArgContainerDbObject.toList(getObject().getArgs());
        }
        return null;
    }

    @Override
    protected void initKeySet() {
        keySet.add("_id");
        keySet.add("taskId");
        keySet.add("method");
        keySet.add("actorId");
        keySet.add("args");
    }

    @Override
    protected TaskContainer createObject() {
        return new TaskContainer();
    }
}
