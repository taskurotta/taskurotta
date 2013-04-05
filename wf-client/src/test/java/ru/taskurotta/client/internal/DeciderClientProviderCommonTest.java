package ru.taskurotta.client.internal;

import org.junit.Test;
import ru.taskurotta.ProxyFactory;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.MemoryBackendBundle;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.server.TaskServer;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 03.04.13
 * Time: 15:03
 */
public class DeciderClientProviderCommonTest {

    @Decider
    static interface TestDecider {
        @Execute
        public void start();
    }

    @DeciderClient(decider = TestDecider.class)
    static interface TestDeciderClient {
        @Execute
        public void start();
    }

    @Test
    public void testGetDeciderClient() throws Exception {
        TestDeciderClient testDeciderClientOrig = ProxyFactory.getDeciderClient(TestDeciderClient.class,
                new RuntimeContext(UUID.randomUUID()));

        BackendBundle backendBundle = new MemoryBackendBundle(0);
        TaskServer taskServer = new GeneralTaskServer(backendBundle);
        DeciderClientProviderCommon deciderClientProviderCommon = new DeciderClientProviderCommon(taskServer);
        TestDeciderClient testDeciderClient = deciderClientProviderCommon.getDeciderClient(TestDeciderClient.class);

        assertEquals(testDeciderClientOrig.getClass(), testDeciderClient.getClass());
    }
}
