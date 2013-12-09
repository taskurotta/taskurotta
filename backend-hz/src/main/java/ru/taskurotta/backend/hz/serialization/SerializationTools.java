package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: greg
 */
public class SerializationTools {

    private static ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();
    private static TaskContainerSerializer taskContainerSerializer = new TaskContainerSerializer();

    public static void writeString(ObjectDataOutput out, String str) throws IOException {
        if (str != null && !str.equals("")) {
            out.writeBoolean(true);
            int size = str.length();
            out.writeInt(size);
            for (char ch : str.toCharArray()) {
                out.writeChar(ch);
            }
        } else {
            out.writeBoolean(false);
        }
    }

    public static String readString(ObjectDataInput in) throws IOException {
        boolean strExist = in.readBoolean();
        if (strExist) {
            int size = in.readInt();
            List<Character> chars = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                chars.add(in.readChar());
            }
            StringBuilder sb = new StringBuilder(chars.size());
            for (Character c : chars)
                sb.append(c);
            return sb.toString();
        } else {
            return null;
        }
    }

    public static void writeArgsContainerArray(ObjectDataOutput out, ArgContainer[] argContainers) throws IOException {
        int argContainersCount = argContainers != null ? argContainers.length : 0;
        out.writeInt(argContainersCount);
        if (argContainersCount > 0) {
            for (int i = 0; i < argContainersCount; i++) {
                argContainerStreamSerializer.write(out, argContainers[i]);
            }
        }
    }

    public static ArgContainer[] readArgsContainerArray(ObjectDataInput in) throws IOException {
        int argContainersCount = in.readInt();
        ArgContainer[] argContainerList = new ArgContainer[argContainersCount];
        for (int i = 0; i < argContainersCount; i++) {
            argContainerList[i] = argContainerStreamSerializer.read(in);
        }
        return argContainerList;
    }

    public static void writeTaskContainerArray(ObjectDataOutput out, TaskContainer[] taskContainers) throws IOException {
        if (taskContainers == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeInt(taskContainers.length);
        for (TaskContainer taskContainer : taskContainers) {
            taskContainerSerializer.write(out, taskContainer);
        }
    }

    public static TaskContainer[] readTaskContainerArray(ObjectDataInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        int taskContainersCount = in.readInt();
        TaskContainer[] tasks = new TaskContainer[taskContainersCount];
        for (int i = 0; i < taskContainersCount; i++) {
            tasks[i] = taskContainerSerializer.read(in);
        }
        return tasks;
    }

    public static void writeStringArray(ObjectDataOutput out, String[] array) throws IOException {
        int length = array == null ? 0 : array.length;
        out.writeInt(length);
        if (length > 0) {
            for (String string : array) {
                writeString(out, string);
            }
        }
    }

    public static String[] readStringArray(ObjectDataInput in) throws IOException {
        int length = in.readInt();
        String[] strings = new String[length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = readString(in);
        }
        return strings;
    }
}
