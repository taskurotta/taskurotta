package ru.taskurotta.backend.ora;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.ora.checkpoint.OracleCheckpointService;
import ru.taskurotta.backend.ora.dao.DbConnect;
import ru.taskurotta.backend.ora.storage.OraProcessBackend;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: moroz
 * Date: 29.04.13
 */
public class ProcessTestIT {

    private DbConnect connection = new DbConnect();
    private OraProcessBackend dao = new OraProcessBackend(connection.getDataSource(), new OracleCheckpointService(connection.getDataSource()));

    @Test
    public void test() {
        TaskContainer task = SerializationTest.createTaskContainer();
        dao.startProcess(task);

        TaskContainer task1 = SerializationTest.createTaskContainer();
        dao.startProcess(task1);

        ProcessVO processVO = dao.getProcess(task1.getProcessId());
        Assert.assertEquals(processVO.getStartTask().getTaskId(), task1.getTaskId());
        GenericPage<ProcessVO> page = dao.listProcesses(1, 100);
        Assert.assertFalse(page.getItems().size() <= 0);
        List<ProcessVO> processVO1 = dao.findProcesses(ProcessInfoRetriever.SEARCH_BY_ID, processVO.getProcessUuid().toString());
        Assert.assertFalse(processVO1.size() <= 0);
    }

}
