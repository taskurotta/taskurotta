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

    private static ArgContainerStreamSerializer argContainerStreamSerializer = new ArgContainerStreamSerializer();
    private static TaskContainerStreamSerializer taskContainerSerializer = new TaskContainerStreamSerializer();

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
        int argContainersCount = (argContainers != null) ? argContainers.length : -1;
        if (argContainers != null && argContainersCount > 0) {
            out.writeInt(argContainersCount);
            for (int i = 0; i < argContainersCount; i++) {
                argContainerStreamSerializer.write(out, argContainers[i]);
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
                argContainerList.add(argContainerStreamSerializer.read(in));
            }
        }
        ArgContainer[] argContainersArray = new ArgContainer[argContainerList.size()];
        argContainerList.toArray(argContainersArray);
        return argContainersArray;
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
        List<TaskContainer> taskContainerList = new ArrayList<>();
        for (int i = 0; i < taskContainersCount; i++) {
            taskContainerList.add(taskContainerSerializer.read(in));
        }
        TaskContainer[] taskContainersArray = new TaskContainer[taskContainerList.size()];
        taskContainerList.toArray(taskContainersArray);
        return taskContainersArray;
    }
}
