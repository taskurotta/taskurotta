package ru.taskurotta.service.ora;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.service.console.model.ProcessVO;
import ru.taskurotta.service.ora.storage.OraProcessService;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: moroz
 * Date: 29.04.13
 */
public class ProcessTestIT {

    private DbConnect connection = new DbConnect();
    private OraProcessService dao = new OraProcessService(connection.getDataSource());

    @Ignore
    @Test
    public void test() {
        TaskContainer task = SerializationTest.createTaskContainer();
        dao.startProcess(task);

        ProcessVO processVO = dao.getProcess(task.getProcessId());
        Assert.assertNull(processVO.getStartTask());
        TaskContainer getedTask = dao.getStartTask(task.getProcessId());
        Assert.assertEquals(task.getTaskId(), getedTask.getTaskId());

    }

}
