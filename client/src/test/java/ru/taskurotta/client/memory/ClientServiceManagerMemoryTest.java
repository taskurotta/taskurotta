package ru.taskurotta.client.memory;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.taskurotta.service.MemoryServiceBundle;
import ru.taskurotta.service.ServiceBundle;
import ru.taskurotta.service.storage.MemoryTaskDao;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;

import static org.junit.Assert.assertNotNull;

/**
 * User: stukushin
 * Date: 03.04.13
 * Time: 13:23
 */
public class ClientServiceManagerMemoryTest {

    private static ClientServiceManagerMemory clientServiceManagerMemory;
    private static ClientServiceManagerMemory clientServiceManagerMemoryWithTaskServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ServiceBundle memoryServiceBundle = new MemoryServiceBundle(60, new MemoryTaskDao());
        TaskServer taskServer = new GeneralTaskServer(memoryServiceBundle);
        clientServiceManagerMemory = new ClientServiceManagerMemory();
        clientServiceManagerMemoryWithTaskServer = new ClientServiceManagerMemory(taskServer);
    }

    @Test
    public void testGetDeciderClientProvider() throws Exception {
        assertNotNull(clientServiceManagerMemory.getDeciderClientProvider());
        assertNotNull(clientServiceManagerMemoryWithTaskServer.getDeciderClientProvider());
    }

    @Test
    public void testGetTaskSpreaderProvider() throws Exception {
        assertNotNull(clientServiceManagerMemory.getTaskSpreaderProvider());
        assertNotNull(clientServiceManagerMemoryWithTaskServer.getTaskSpreaderProvider());
    }
}
