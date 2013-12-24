package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.IOException;

/**
 * User: greg
 */
public class SerializationTools {

    private static ArgContainerStreamSerializer argContainerSerializer = new ArgContainerStreamSerializer();
    private static TaskContainerStreamSerializer taskContainerSerializer = new TaskContainerStreamSerializer();

    public static void writeString(ObjectDataOutput out, String str) throws IOException {
        if (str == null) {
            out.writeBoolean(false);
            return;
        }

        out.writeBoolean(true);

        out.writeUTF(str);
    }

    public static String readString(ObjectDataInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }

        return in.readUTF();
    }

    public static void writeArgsContainerArray(ObjectDataOutput out, ArgContainer[] argContainers) throws IOException {
        int argContainersCount = argContainers != null ? argContainers.length : 0;
        out.writeInt(argContainersCount);
        if (argContainersCount > 0) {
            for (int i = 0; i < argContainersCount; i++) {
                argContainerSerializer.write(out, argContainers[i]);
            }
        }
    }

    public static ArgContainer[] readArgsContainerArray(ObjectDataInput in) throws IOException {
        int argContainersCount = in.readInt();
        ArgContainer[] argContainerList = new ArgContainer[argContainersCount];
        for (int i = 0; i < argContainersCount; i++) {
            argContainerList[i] = argContainerSerializer.read(in);
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
        int length = array == null ? -1 : array.length;
        out.writeInt(length);
        if (length > 0) {
            for (String string : array) {
                writeString(out, string);
            }
        }
    }

    public static String[] readStringArray(ObjectDataInput in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        String[] strings = new String[length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = readString(in);
        }
        return strings;
    }
}
