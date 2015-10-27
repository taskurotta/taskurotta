package ru.taskurotta.service.hz.storage;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 23.10.2015
 * Time: 16:24
 */

@Ignore // only for manual test
public class MongoProcessServiceTest {

    private MongoProcessService mongoProcessService;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.load(MongoProcessServiceTest.class.getClassLoader().getResourceAsStream("default.properties"));

        String taskurottaTestMongoDBName = "taskurotta-test";
        properties.setProperty("mongo.db.name", taskurottaTestMongoDBName);

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                new String[]{"spring/hz-mongo.xml"}, false);
        applicationContext.getEnvironment().getPropertySources().addLast(
                new PropertiesPropertySource("customProperties", properties));
        applicationContext.refresh();

        mongoProcessService = applicationContext.getBean("processService", MongoProcessService.class);
    }

    @Test
    public void testFindLostProcesses() throws Exception {
        UUID finishedProcessId = UUID.randomUUID();
        UUID abortedProcessId = UUID.randomUUID();

        mongoProcessService.startProcess(createTaskContainer(finishedProcessId));
        mongoProcessService.startProcess(createTaskContainer(abortedProcessId));

        mongoProcessService.finishProcess(finishedProcessId, null);
        mongoProcessService.markProcessAsAborted(abortedProcessId);

        long now = System.currentTimeMillis();
        ResultSetCursor<UUID> resultSetCursor = mongoProcessService.findLostProcesses(now + 1000L, now + 1000L, 100);
        Collection<UUID> collection = resultSetCursor.getNext();
        assertEquals(2, collection.size());
    }

    private TaskContainer createTaskContainer(UUID processId) {
        return new TaskContainer(
                UUID.randomUUID(),
                processId,
                null,
                "testMethod",
                "TestActor#1.0",
                TaskType.DECIDER_START,
                0L,
                0,
                new ArgContainer[] {},
                new TaskOptionsContainer(),
                false,
                new String[] {}
        );
    }
}