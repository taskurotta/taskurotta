package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 03/02/15.
 */
public final class SerializerTools {

    private static final int ID_CACHE_SIZE = 1000;
    private static final CString[] ARRAY_INDEXES = new CString[ID_CACHE_SIZE];

    static {
        for (int i = 0; i < ARRAY_INDEXES.length; i++) {
            ARRAY_INDEXES[i] = new CString(Integer.toString(i));
        }
    }



    public static void writeListOfString(CString name, List<String> list, BDataOutput out) {
        int label = out.writeArray(name);
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            out.writeString(i, s);
        }
        out.writeArrayStop(label);
    }

    public static CString createCString(int i) {
        if (i >= 1000) {
            return new CString(Integer.toString(i));
        } else
            return ARRAY_INDEXES[i];
    }

    public static List<String> readListOfString(CString name, BDataInput in) {
        int label = in.readArray(name);
        int size = in.readArraySize();
        List<String> list = null;
        if (label != -1) {
            if (size > 0) {
                list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(in.readString(i));
                }
            }
            in.readArrayStop(label);
        }
        return list;
    }

}
