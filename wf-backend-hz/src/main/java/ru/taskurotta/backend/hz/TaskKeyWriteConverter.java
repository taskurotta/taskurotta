package ru.taskurotta.backend.hz;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 08.08.13 19:21
 */
public class TaskKeyWriteConverter implements Converter<TaskKey, DBObject> {

    @Override
    public DBObject convert(TaskKey source) {
        DBObject dbo = new BasicDBObject();
        dbo.put("_id", source.getTaskId());
        dbo.put("taskId", source.getTaskId());
        dbo.put("processId", source.getProcessId());
        return dbo;
    }

}
