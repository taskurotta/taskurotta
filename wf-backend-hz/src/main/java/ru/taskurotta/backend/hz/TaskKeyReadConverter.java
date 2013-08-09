package ru.taskurotta.backend.hz;

import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

/**
 * User: dimadin
 * Date: 08.08.13 19:16
 */
public class TaskKeyReadConverter implements Converter<DBObject, TaskKey> {

    @Override
    public TaskKey convert(DBObject source) {
        UUID taskId  = (UUID)source.get("taskId");
        UUID processId  = (UUID)source.get("processId");
        return new TaskKey(processId, taskId);
    }

}
