package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: greg
 */
public class SerializationTools {


    public static void writeString(ObjectDataOutput out, String str) throws IOException {
        int size = str.length();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeChar(str.charAt(i));
        }
    }

    public static String readString(ObjectDataInput in) throws IOException {
        int size = in.readInt();
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            chars.add(in.readChar());
        }
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars)
            sb.append(c);
        return sb.toString();
    }

}
