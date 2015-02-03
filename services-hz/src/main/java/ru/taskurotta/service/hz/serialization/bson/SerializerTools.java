package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;

/**
 * Created by greg on 03/02/15.
 */
public final class SerializerTools {


    public static void writeArrayOfString(CString name, String[] array, BDataOutput out) {
        int label = out.writeArray(name);
        for (int i = 0; i < array.length; i++) {
            String s = array[i];
            out.writeString(i, s);
        }
        out.writeArrayStop(label);
    }

    public static String[] readArrayOfString(CString name, BDataInput in) {
        int label = in.readArray(name);
        int size = in.readArraySize();
        String[] array = null;
        if (size > 0) {
            array = new String[size];
            for (int i = 0; i < size; i++) {
                array[i] = in.readString(i);
            }
        }
        in.readArrayStop(label);
        return array;
    }

}
