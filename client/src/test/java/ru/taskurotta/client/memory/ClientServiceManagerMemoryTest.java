package ru.taskurotta.client.memory;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.backend.storage.MemoryTaskDao;
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
        BackendBundle memoryBackendBundle = new MemoryBackendBundle(60, new MemoryTaskDao());
        TaskServer taskServer = new GeneralTaskServer(memoryBackendBundle);
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
