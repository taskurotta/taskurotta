package ru.taskurotta.hz.test.mongo.serialization.custom;

import com.mongodb.DBCollection;
import com.mongodb.DefaultDBCallback;

/**
 * Created by greg on 27/01/15.
 */
public class CustomDBCallback extends DefaultDBCallback {


    public CustomDBCallback(DBCollection coll) {
        super(coll);
    }

    @Override
    public void arrayStart(String name) {
        System.out.println(name);
        super.arrayStart(name);
    }
}
