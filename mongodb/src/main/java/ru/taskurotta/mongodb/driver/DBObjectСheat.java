package ru.taskurotta.mongodb.driver;

import com.mongodb.DBObject;
import org.bson.BSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 */
public class DBObjectСheat<T> implements DBObject {

    final private T obj;

    public DBObjectСheat(T obj) {
        this.obj = obj;
    }

    public T getObject() {
        return obj;
    }

    @Deprecated
    @Override
    public void markAsPartialObject() {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public boolean isPartialObject() {
        return false;
    }

    @Deprecated
    @Override
    public Object put(String key, Object v) {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public void putAll(BSONObject o) {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public void putAll(Map m) {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public Object get(String key) {
        if (key.equals("_id")) {
            return "";
        }

        // BDecoder detects error objects self
        if (key.equals("$err") || key.equals("err") || key.equals("errmsg")) {
            return null;
        }

        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public Map toMap() {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public Object removeField(String key) {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public boolean containsKey(String s) {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public boolean containsField(String s) {
        throw new IllegalStateException("Not supported!");
    }

    @Deprecated
    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
//        throw new IllegalStateException("Not supported!");
    }

}