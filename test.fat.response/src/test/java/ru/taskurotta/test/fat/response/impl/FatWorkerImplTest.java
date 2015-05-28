package ru.taskurotta.test.fat.response.impl;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.test.fat.response.Response;

/**
 * Created on 28.05.2015.
 */
public class FatWorkerImplTest {

    int size = 1024 * 1024;

    @Test
    public void testCeateMessage() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        Response resp = target.createResponse(size);
        Assert.assertEquals(size, resp.getMessage().length());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailMessage() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        Response resp = target.createResponse(-size);
    }

    @Test
    public void testFailMessageSize() throws Exception {
        FatWorkerImpl target = new FatWorkerImpl();
        try {
            Response resp = target.createResponse(-size);
        } catch(Exception e) {
            Assert.assertEquals(size, e.getMessage().length());
        }
    }


}
