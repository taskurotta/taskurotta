package ru.taskurotta.service.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 03.04.2014
 * Time: 19:32
 */

public class GeneralTaskServiceTest {

    private TaskDao taskDao;
    private GeneralTaskService generalTaskService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        this.taskDao = new MemoryTaskDao();
        this.generalTaskService = new GeneralTaskService(taskDao);
    }

    @Test
    public void testGetTaskToExecute() throws Exception {
        UUID processId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        List<ArgContainer> argContainerArrayList = new ArrayList<>();

        ArgContainer argContainer1 = new ArgContainer();
        argContainer1.setTaskId(UUID.randomUUID());
        argContainer1.setClassName(ArrayList.class.getName());
        argContainer1.setJSONValue(objectMapper.writeValueAsString(new ArrayList<String>()));
        argContainer1.setPromise(false);
        argContainer1.setReady(true);
        argContainer1.setType(ArgContainer.ValueType.COLLECTION);

        argContainerArrayList.add(argContainer1);

        ArgContainer[] argContainers = new ArgContainer[argContainerArrayList.size()];
        argContainerArrayList.toArray(argContainers);

        String[] failTypes = {"java.lang.RuntimeException"};

        TaskContainer taskContainer = new TaskContainer(taskId, processId, "start", "actorId", TaskType.DECIDER_START,
                0l, 0, argContainers, new TaskOptionsContainer(), false, failTypes);

        taskDao.addTask(taskContainer);

        TaskContainer resultTaskContainer = generalTaskService.getTaskToExecute(taskId, processId);

        assertEquals(taskContainer, resultTaskContainer);
    }
}
