package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.DBEncoder;
import org.bson.BSONObject;
import org.bson.BasicBSONEncoder;
import org.bson.io.OutputBuffer;
import org.bson.types.ObjectId;

/**
 * Created by greg on 23/01/15.
 */
public class CustomBasicBSONEncoder extends BasicBSONEncoder implements DBEncoder {

    @Override
    protected void _putObjectField(String name, Object val) {
        super._putObjectField(name, val);
    }

    @Override
    protected void putObjectId(String name, ObjectId oid) {
        super.putObjectId(name, oid);
    }

    @Override
    public int writeObject(OutputBuffer buf, BSONObject o) {
        set(buf);
        int x = super.putObject(o);
        done();
        return x;
    }


}
