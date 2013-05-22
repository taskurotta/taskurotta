package ru.taskurotta.client.external;

import org.junit.Test;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

/**
 * User: stukushin
 * Date: 22.05.13
 * Time: 16:35
 */
public class JerseyDeciderClientProviderTest {

    @Decider
    interface TestDecider {
        @Execute
        public void start();
    }

    @DeciderClient(decider = TestDecider.class)
    interface TestDeciderClient {
        public void start();
    }

    @Test
    public void testSend() {
        JerseyDeciderClientProvider jerseyDeciderClientProvider = new JerseyDeciderClientProvider("http://localhost:8080");
        TestDeciderClient testDeciderClient = jerseyDeciderClientProvider.getDeciderClient(TestDeciderClient.class);
        testDeciderClient.start();
    }
}
