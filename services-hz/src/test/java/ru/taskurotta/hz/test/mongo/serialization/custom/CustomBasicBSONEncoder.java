package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.DBEncoder;
import org.bson.BSONObject;
import org.bson.BasicBSONEncoder;
import org.bson.io.OutputBuffer;

/**
 * Created by greg on 23/01/15.
 */
public class CustomBasicBSONEncoder extends BasicBSONEncoder implements DBEncoder {

    @Override
    protected void _putObjectField(String name, Object val) {
        if (name.equals("task-container")) {
            System.out.println("Yeap! I'm here");
        } else {
            super._putObjectField(name, val);
        }
    }

    @Override
    public int writeObject(OutputBuffer buf, BSONObject o) {
        set(buf);
        int x = super.putObject(o);
        done();
        return x;
    }
}
