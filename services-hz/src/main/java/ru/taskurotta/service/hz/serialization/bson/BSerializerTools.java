package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.ArrayList;
import java.util.List;

public final class BSerializerTools {

    public static interface ArrayFactory<T> {
        public T[] create(int size);
    }

    public static <T> Void writeObjectIfNotNull(int index, T object, StreamBSerializer<T> serializer, BDataOutput out) {
        if (object == null) {
            return null;
        }

        int objectLabel = out.writeObject(index);
        serializer.write(out, object);
        out.writeObjectStop(objectLabel);

        return null;
    }

    public static <T> Void writeObjectIfNotNull(CString name, T object, StreamBSerializer<T> serializer, BDataOutput out) {
        if (object == null) {
            return null;
        }

        int objectLabel = out.writeObject(name);
        serializer.write(out, object);
        out.writeObjectStop(objectLabel);

        return null;
    }

    public static <T> T readObject(CString name, StreamBSerializer<T> serializer, BDataInput in) {
        int objLabel = in.readObject(name);
        if (objLabel == -1) {
            return null;
        }

        T obj = serializer.read(in);
        in.readObjectStop(objLabel);

        return obj;
    }

    public static <T> T readObject(int index, StreamBSerializer<T> serializer, BDataInput in) {
        int objLabel = in.readObject(index);
        if (objLabel == -1) {
            return null;
        }

        T obj = serializer.read(in);
        in.readObjectStop(objLabel);

        return obj;
    }

    public static <T> Void writeArrayOfObjectsIfNotEmpty(CString name, T[] array, StreamBSerializer<T> serializer,
                                                         BDataOutput out) {

        if (array == null || array.length == 0) {
            return null;
        }

        return writeArrayOfObjects(name, array, serializer, out);
    }

    public static <T> Void writeArrayOfObjects(CString name, T[] array, StreamBSerializer<T> serializer, BDataOutput
            out) {

        if (array == null) {
            return null;
        }

        int arrayLabel = out.writeArray(name);
        for (int i = 0; i < array.length; i++) {
            int objectLabel = out.writeObject(i);
            serializer.write(out, array[i]);
            out.writeObjectStop(objectLabel);
        }
        out.writeArrayStop(arrayLabel);

        return null;
    }

    public static <T> T[] readArrayOfObjects(CString name, ArrayFactory<T> arrayF, StreamBSerializer<T> serializer,
                                             BDataInput in) {

        int arrayLabel = in.readArray(name);
        if (arrayLabel == -1) {
            return null;
        }

        int arraySize = in.readArraySize();

        T[] objects = arrayF.create(arraySize);
        for (int i = 0; i < arraySize; i++) {
            objects[i] = readObject(i, serializer, in);
        }
        in.readArrayStop(arrayLabel);

        return objects;
    }

    public static void writeArrayOfString(CString name, String[] array, BDataOutput out) {
        if (array == null) {
            return;
        }

        int arrayLabel = out.writeArray(name);
        for (int i = 0; i < array.length; i++) {
            out.writeString(i, array[i]);
        }
        out.writeArrayStop(arrayLabel);
    }

    public static String[] readArrayOfString(CString name, BDataInput in) {
        int arrayLabel = in.readArray(name);
        if (arrayLabel == -1) {
            return null;
        }

        int arraySize = in.readArraySize();

        String[] array = new String[arraySize];
        for (int i = 0; i < arraySize; i++) {
            array[i] = in.readString(i);
        }
        in.readArrayStop(arrayLabel);

        return array;
    }

    public static void writeListOfString(CString name, List<String> list, BDataOutput out) {
        if (list == null) {
            return;
        }

        int label = out.writeArray(name);
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            out.writeString(i, s);
        }
        out.writeArrayStop(label);
    }

    public static List<String> readListOfString(CString name, BDataInput in, List<String> defValue) {
        int label = in.readArray(name);
        if (label == -1) {
            return defValue;
        }

        int size = in.readArraySize();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(in.readString(i));
        }
        in.readArrayStop(label);

        return list;
    }

}
