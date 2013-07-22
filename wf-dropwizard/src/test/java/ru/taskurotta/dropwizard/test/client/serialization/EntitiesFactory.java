package ru.taskurotta.dropwizard.test.client.serialization;

import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

public class EntitiesFactory {

    public static ActorDefinition createActorDefinition() {
        return ActorDefinition.valueOf("test.me.worker", "7.6.5");
    }

    public static TaskContainer createTaskContainer() {
        UUID originalUuid = UUID.randomUUID();
        UUID processUuid = UUID.randomUUID();
        TaskType originalTaskType = TaskType.WORKER;
        String originalName = "test.me.worker";
        String originalVersion = "7.6.5";
        String originalMethod = "doSomeWork";
        String originalActorId = originalName + "#" + originalVersion;
        long originalStartTime = System.currentTimeMillis();
        int originalNumberOfAttempts = 5;

        String origArg1ClassName = "null";
        String origArg1Value = "null";
        ArgContainer originalArg1 = new ArgContainer(origArg1ClassName, ArgContainer.ValueType.PLAIN, originalUuid, false, true, origArg1Value);

        String origArg2ClassName = "java.lang.String";
        String origArg2Value = "string value here";
        ArgContainer originalArg2 = new ArgContainer(origArg2ClassName, ArgContainer.ValueType.PLAIN, originalUuid, true, false, origArg2Value);


        ArgType[] argTypes = new ArgType[]{ArgType.WAIT, ArgType.NONE};
        TaskOptionsContainer originalOptions = new TaskOptionsContainer(argTypes);

        return new TaskContainer(originalUuid, processUuid, originalMethod, originalActorId, originalTaskType,originalStartTime, originalNumberOfAttempts, new ArgContainer[]{originalArg1, originalArg2}, originalOptions);
    }

    public static DecisionContainer createDecisionContainer(boolean isError) {
        UUID taskId =UUID.randomUUID();
        UUID processId =UUID.randomUUID();
        TaskContainer[] tasks = new TaskContainer[2];
        tasks[0] = createTaskContainer();
        tasks[1] = createTaskContainer();
        if (isError) {
            return new DecisionContainer(taskId, processId, null, createErrorContainer(), System.currentTimeMillis()+9000l, tasks, null);
        } else {
            return new DecisionContainer(taskId, processId, createArgSimpleValue(taskId), null, TaskDecision.NO_RESTART, tasks, null);
        }

    }

    public static ErrorContainer createErrorContainer() {
        ErrorContainer result = new ErrorContainer();
        result.setClassName(Throwable.class.getName());
        result.setMessage("Test exception");
        result.setStackTrace("Test stack trace");
        return result;
    }

    public static ArgContainer createArgSimpleValue(UUID taskId) {
        String value = "simple string value";
        return new ArgContainer(value.getClass().getName(), ArgContainer.ValueType.PLAIN, taskId, true, false, value);
    }

}
