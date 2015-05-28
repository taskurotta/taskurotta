package ru.taskurotta.test.fat.response.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileCopyUtils;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.test.fat.response.Response;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.File;
import java.util.UUID;

/**
 * Created on 28.05.2015.
 */
public class FatWorkerImplTest {

    int size = 1024 * 1024;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testCeateMessage() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        String resp = target.createResponse(size);
        Assert.assertEquals(size, resp.length());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailMessage() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        target.createResponse(-size);
    }

    @Test
    public void testFailMessageSize() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        try {
            target.createResponse(-size);
        } catch(Exception e) {
            Assert.assertEquals(size, e.getMessage().length());
        }
    }

    @Test
    public void testWriteToFile() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        String result = target.createResponse(size);

        File file = tmp.newFile();

        FileCopyUtils.copy(result.getBytes(), file);

        Assert.assertEquals(size, file.length());

    }

    @Test
    public void testWriteJsonToFile() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        final String result = target.createResponse(size);
        File file1 = tmp.newFile();
//        File file2 = tmp.newFile();

        DecisionContainer container1 = getContainer(result);
//        DecisionContainer container2 = getContainer(result);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(file1, container1);
//        mapper.writeValue(file2, container2);

        System.out.println("File1 size: " + file1.length());

    }


    DecisionContainer getContainer(final Object value) {
        ObjectFactory factory = new ObjectFactory();

        TaskDecision dcsn = new TaskDecision() {
            @Override
            public UUID getId () {
                return UUID.randomUUID();
            }

            @Override
            public UUID getProcessId () {
                return UUID.randomUUID();
            }

            @Override
            public Object getValue () {
                return value;
            }

            @Override
            public Task[] getTasks () {
                return null;
            }

            @Override
            public boolean isError () {
                return false;
            }

            @Override
            public Throwable getException () {
                return null;
            }

            @Override
            public long getRestartTime () {
                return 0;
            }

            @Override
            public long getExecutionTime () {
                return 0;
            }

            @Override
            public UUID getPass () {
                return UUID.randomUUID();
            }
        };
        return factory.dumpResult(dcsn, "test");
    }


}
