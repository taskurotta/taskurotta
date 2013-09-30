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

    public static void writeTaskContainerArray(ObjectDataOutput out, TaskContainer[] taskContainers) throws IOException {
        int taskContainersCount = taskContainers.length;
        if (taskContainers != null && taskContainersCount > 0) {
            out.writeInt(taskContainersCount);
            for (int i = 0; i < taskContainersCount; i++) {
                taskContainerSerializer.write(out, taskContainers[i]);
            }
        } else {
            out.writeInt(-1);
        }
    }

    public static TaskContainer[] readTaskContainerArray(ObjectDataInput in) throws IOException {
        int taskContainersCount = in.readInt();
        List<TaskContainer> taskContainerList = new ArrayList<>();
        if (taskContainersCount != -1) {
            for (int i = 0; i < taskContainersCount; i++) {
                taskContainerList.add(taskContainerSerializer.read(in));
            }
        }
        TaskContainer[] taskContainersArray = new TaskContainer[taskContainerList.size()];
        taskContainerList.toArray(taskContainersArray);
        return taskContainersArray;
    }
}
