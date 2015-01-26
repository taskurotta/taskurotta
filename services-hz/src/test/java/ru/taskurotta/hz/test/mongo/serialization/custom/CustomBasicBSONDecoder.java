package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.*;
import org.bson.BasicBSONDecoder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by greg on 26/01/15.
 */
public class CustomBasicBSONDecoder extends BasicBSONDecoder implements DBDecoder {


    @Override
    public DBCallback getDBCallback(DBCollection collection) {
        // brand new callback every time
        return new DefaultDBCallback(collection);
    }

    @Override
    public DBObject decode(byte[] b, DBCollection collection) {
        DBCallback cbk = getDBCallback(collection);
        cbk.reset();
        decode(b, cbk);
        return (DBObject) cbk.get();
    }

    @Override
    public DBObject decode(InputStream in,  DBCollection collection) throws IOException {
        DBCallback cbk = getDBCallback(collection);
        cbk.reset();
        decode(in, cbk);
        return (DBObject) cbk.get();
    }
}
