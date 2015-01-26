package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.DBEncoder;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONEncoder;
import org.bson.io.OutputBuffer;
import org.bson.types.ObjectId;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * Created by greg on 23/01/15.
 */
public class CustomBasicBSONEncoder extends BasicBSONEncoder implements DBEncoder {

    @Override
    protected void _putObjectField(String name, Object val) {
        if (val.getClass().getName().equals("ru.taskurotta.transport.model.TaskContainer")) {
            _putTaskContainer(name, (TaskContainer) val);
        } else {
            super._putObjectField(name, val);
        }
    }

    @Override
    protected void putObjectId(String name, ObjectId oid) {
        super.putObjectId(name, oid);
    }

    private void _putTaskContainer(String name, TaskContainer val) {
        val = (TaskContainer) BSON.applyEncodingHooks(val);
        putString(name+".actorId", val.getActorId());
        putString(name+".method", val.getMethod());
    }

    @Override
    public int writeObject(OutputBuffer buf, BSONObject o) {
        set(buf);
        int x = super.putObject(o);
        done();
        return x;
    }


}
