package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;

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


    public static void writeArrayOfString(CString name, String[] array, BDataOutput out) {
        int label = out.writeArray(name);
        for (int i = 0; i < array.length; i++) {
            String s = array[i];
            out.writeString(i, s);
        }
        out.writeArrayStop(label);
    }

    public static CString createCString(int i){
        if (i>=1000) {
            return new CString(Integer.toString(i));
        } else
            return ARRAY_INDEXES[i];
    }

    public static String[] readArrayOfString(CString name, BDataInput in) {
        int label = in.readArray(name);
        int size = in.readArraySize();
        String[] array = null;
        if (label != -1) {
            if (size > 0) {
                array = new String[size];
                for (int i = 0; i < size; i++) {
                    array[i] = in.readString(i);
                }
            }
            in.readArrayStop(label);
        }
        return array;
    }

}
