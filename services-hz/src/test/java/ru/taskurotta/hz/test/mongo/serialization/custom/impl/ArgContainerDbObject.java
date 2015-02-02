package ru.taskurotta.hz.test.mongo.serialization.custom.impl;

import org.bson.types.ObjectId;
import ru.taskurotta.hz.test.mongo.serialization.custom.CustomDBObject;
import ru.taskurotta.transport.model.ArgContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by greg on 27/01/15.
 */
public class ArgContainerDbObject extends CustomDBObject<ArgContainer> {

    @Override
    protected void initKeySet() {
        keySet.add("dataType");
        keySet.add("taskId");
    }

    @Override
    protected ArgContainer createObject() {
        return new ArgContainer();
    }

    @Override
    public Object put(String key, Object v) {
        switch (key) {
            case "_id":
                objectId = (ObjectId) v;
                return v;
            case "taskId":
                getObject().setTaskId(UUID.fromString((String) v));
                return v;
            case "dataType":
                getObject().setDataType((String) v);
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
            case "dataType":
                return getObject().getDataType();
        }
        return null;
    }

    public static List<ArgContainerDbObject> toList(ArgContainer[] args) {
        List<ArgContainerDbObject> dbObjects = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            ArgContainer arg = args[i];
            ArgContainerDbObject argContainerDbObject = new ArgContainerDbObject();
            argContainerDbObject.setObject(arg);
            dbObjects.add(argContainerDbObject);
        }
        return dbObjects;
    }

    public static ArgContainer[] fromArray(ArgContainerDbObject[] args) {
        ArgContainer[] argContainers = new ArgContainer[args.length];
        for (int i = 0; i < args.length; i++) {
            ArgContainerDbObject arg = args[i];
            argContainers[i] = arg.getObject();
        }
        return argContainers;
    }
}
