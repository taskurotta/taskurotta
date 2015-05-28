package ru.taskurotta.test.fat.response.impl;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileCopyUtils;
import ru.taskurotta.test.fat.response.Response;

import java.io.File;

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
        byte[] resp = target.createResponse(size);
        Assert.assertEquals(size, resp.length);
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
        byte[] result = target.createResponse(size);

        File file = tmp.newFile();

        FileCopyUtils.copy(result, file);

        Assert.assertEquals(size, file.length());

    }


}
