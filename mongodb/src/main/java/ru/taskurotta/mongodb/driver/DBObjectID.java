package ru.taskurotta.mongodb.driver;

import com.mongodb.DBObject;
import org.bson.BSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 */
public class DBObjectID implements DBObject {

    private Object id;

    public DBObjectID(Object id) {
        this.id = id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    @Override
    public void markAsPartialObject() {
    }

    @Override
    public boolean isPartialObject() {
        return true;
    }

    @Override
    public Object put(String key, Object v) {
        return null;
    }

    @Override
    public void putAll(BSONObject o) {
    }

    @Override
    public void putAll(Map m) {
    }

    @Override
    public Object get(String key) {
        if (key.equals("_id")) {
            return id;
        }
        return null;
    }

    @Override
    public Map toMap() {
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public Object removeField(String key) {
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public boolean containsKey(String s) {
        if (s.equals("_id")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean containsField(String s) {
        if (s.equals("_id")) {
            return true;
        }
        return false;
    }


    private Set<String> oneKeySet =  new Set<String>() {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            if (o.equals("_id")) {
                return true;
            }
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {

                boolean isEmpty = false;

                @Override
                public boolean hasNext() {
                    return !isEmpty;
                }

                @Override
                public String next() {
                    if (isEmpty) {
                        return null;
                    }

                    isEmpty = true;
                    return "_id";
                }

                @Override
                public void remove() {
                    throw new IllegalStateException("Not supported!");
                }
            };
        }

        @Override
        public Object[] toArray() {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public boolean add(String s) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public boolean remove(Object o) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public void clear() {
            throw new IllegalStateException("Not supported!");
        }
    };

    @Override
    public Set<String> keySet() {
        return oneKeySet;
    }
}
