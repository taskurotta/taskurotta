package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 03/02/15.
 */
public final class BSerializerTools {

    private static final int ID_CACHE_SIZE = 1000;
    private static final CString[] ARRAY_INDEXES = new CString[ID_CACHE_SIZE];

    static {
        for (int i = 0; i < ARRAY_INDEXES.length; i++) {
            ARRAY_INDEXES[i] = new CString(Integer.toString(i));
        }
    }

    public static <T> Void writeObjectIfNotNull(CString name, T object, StreamBSerializer<T> serializer, BDataOutput out) {
        if (object != null) {
            int objectLabel = out.writeObject(name);
            serializer.write(out, object);
            out.writeObjectStop(objectLabel);
        }
        return null;
    }

    public static <T> T readObject(CString name, StreamBSerializer<T> serializer, BDataInput in){
        int objLabel = in.readObject(name);
        T obj = null;
        if (objLabel != -1) {
            obj = serializer.read(in);
            in.readObjectStop(objLabel);
        }
        return obj;
    }

    public static void writeArrayOfString(CString name, String[] array, BDataOutput out) {
        if (array != null) {
            int arrayLabel = out.writeArray(name);
            for (int i = 0; i < array.length; i++) {
                String s = array[i];
                out.writeString(i, s);
            }
            out.writeArrayStop(arrayLabel);
        }
    }

    public static String[] readArrayOfString(CString name, BDataInput in) {
        int arrayLabel = in.readArray(name);
        String[] array = null;
        if (arrayLabel != -1) {
            int arraySize = in.readArraySize();
            array = new String[arraySize];
            for (int i = 0; i < arraySize; i++) {
                array[i] = in.readString(i);
            }
            in.readArrayStop(arrayLabel);
        }
        return array;
    }

    public static CString createCString(int i) {
        if (i >= 1000) {
            return new CString(Integer.toString(i));
        } else
            return ARRAY_INDEXES[i];
    }

    public static void writeListOfString(CString name, List<String> list, BDataOutput out) {
        int label = out.writeArray(name);
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            out.writeString(i, s);
        }
        out.writeArrayStop(label);
    }

    public static List<String> readListOfString(CString name, BDataInput in) {
        int label = in.readArray(name);
        int size = in.readArraySize();
        List<String> list;
        if (label != -1) {
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(in.readString(i));
            }
            in.readArrayStop(label);
        } else {
            list = new ArrayList<>(0);
        }
        return list;
    }

}
