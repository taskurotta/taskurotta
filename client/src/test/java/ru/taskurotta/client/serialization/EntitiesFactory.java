package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.*;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class EntitiesFactory {

    public static TaskContainer createTaskContainer() {
        UUID originalUuid = UUID.randomUUID();
        UUID processUuid = UUID.randomUUID();
        TaskType originalTaskType = TaskType.WORKER;
        String originalName = "test.me.worker";
        String originalVersion = "7.6.5";
        String originalMethod = "doSomeWork";
        String originalActorId = originalName + "#" + originalVersion;
        long originalStartTime = System.currentTimeMillis();
        int originalErrorAttempts = 5;

        String origArg1ClassName = "null";
        String origArg1Value = null;
        ArgContainer originalArg1 = new ArgContainer(origArg1ClassName, ArgContainer.ValueType.PLAIN, originalUuid,
                false, true, origArg1Value);

        String origArg2ClassName = "java.lang.String";
        String origArg2Value = "\"string value here\"";
        ArgContainer originalArg2 = new ArgContainer(origArg2ClassName, ArgContainer.ValueType.PLAIN, originalUuid,
                true, false, origArg2Value);


        ArgType[] argTypes = new ArgType[]{ArgType.WAIT, ArgType.NONE};
        TaskOptionsContainer originalOptions = new TaskOptionsContainer(argTypes);

        return new TaskContainer(originalUuid, processUuid, UUID.randomUUID(), originalMethod, originalActorId,
                originalTaskType, originalStartTime, originalErrorAttempts,
                new ArgContainer[]{originalArg1, originalArg2}, originalOptions, false, null);
    }

    public static DecisionContainer createDecisionContainer(boolean isError) {
        UUID taskId = UUID.randomUUID();
        UUID processId =UUID.randomUUID();
        TaskContainer[] tasks = new TaskContainer[2];
        tasks[0] = createTaskContainer();
        tasks[1] = createTaskContainer();
        if (isError) {
            return new DecisionContainer(taskId, processId, null, null, createErrorContainer(), System
                    .currentTimeMillis() +9000l, tasks, null, 0l);
        } else {
            return new DecisionContainer(taskId, processId, null, createArgSimpleValue(taskId), null, TaskDecision
                    .NO_RESTART, tasks, null, 0l);
        }

    }

    public static ErrorContainer createErrorContainer() {
        return new ErrorContainer(new Throwable("Test exception"));
    }

    public static ArgContainer createArgSimpleValue(UUID taskId) {
        String value = "\"simple string value\"";
        return new ArgContainer(value.getClass().getName(), ArgContainer.ValueType.PLAIN, taskId, true, false, value);
    }

    public static ArgContainer createArgPojoValue(UUID taskId) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        SimplePojo sp = new SimplePojo();
        sp.setDateVal(new Date());
        sp.setLongVal(123456l);
        sp.setStringVal("my_value_here");

        return new ArgContainer(sp.getClass().getName(), ArgContainer.ValueType.PLAIN, taskId, true, false, om.writeValueAsString(sp));
    }

    public static class SimplePojo implements Serializable {
        private String stringVal;
        private long longVal;
        private Date dateVal;

        public String getStringVal() {
            return stringVal;
        }

        public void setStringVal(String stringVal) {
            this.stringVal = stringVal;
        }

        public long getLongVal() {
            return longVal;
        }

        public void setLongVal(long longVal) {
            this.longVal = longVal;
        }

        public Date getDateVal() {
            return dateVal;
        }

        public void setDateVal(Date dateVal) {
            this.dateVal = dateVal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimplePojo that = (SimplePojo) o;

            if (longVal != that.longVal) return false;
            if (dateVal != null ? !dateVal.equals(that.dateVal) : that.dateVal != null) return false;
            if (stringVal != null ? !stringVal.equals(that.stringVal) : that.stringVal != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = stringVal != null ? stringVal.hashCode() : 0;
            result = 31 * result + (int) (longVal ^ (longVal >>> 32));
            result = 31 * result + (dateVal != null ? dateVal.hashCode() : 0);
            return result;
        }
    }


}
