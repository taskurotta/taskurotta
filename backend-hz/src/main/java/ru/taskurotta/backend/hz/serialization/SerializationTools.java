package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import ru.taskurotta.transport.model.ArgContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: greg
 */
public class SerializationTools {

    private static ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();

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


    public static void writeArgsContainerArray(ObjectDataOutput out, ArgContainer[] argContainers) throws IOException {
        int argContainersCount = argContainers.length;
        if (argContainers != null && argContainersCount > 0) {
            out.writeInt(argContainersCount);
            for (int i = 0; i < argContainersCount; i++) {
                argContainerSerializer.write(out, argContainers[i]);
            }
        } else {
            out.writeInt(-1);
        }
    }

    public static ArgContainer[] readArgsContainerArray(ObjectDataInput in) throws IOException {
        int argContainersCount = in.readInt();
        List<ArgContainer> argContainerList = new ArrayList<>();
        if (argContainersCount != -1) {
            for (int i = 0; i < argContainersCount; i++) {
                argContainerList.add(argContainerSerializer.read(in));
            }
        }
        ArgContainer[] argContainersArray = new ArgContainer[argContainerList.size()];
        argContainerList.toArray(argContainersArray);
        return argContainersArray;
    }
}
