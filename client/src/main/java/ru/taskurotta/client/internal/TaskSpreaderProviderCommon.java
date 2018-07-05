package ru.taskurotta.client.internal;

import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:30
 */
public class TaskSpreaderProviderCommon implements TaskSpreaderProvider {

    private TaskServer taskServer;
    private ObjectFactory objectFactory;

    public TaskSpreaderProviderCommon(TaskServer taskServer, ObjectFactory objectFactory) {
        this.taskServer = taskServer;
        this.objectFactory = objectFactory;
    }

    @Override
    public TaskSpreader getTaskSpreader(ActorDefinition actorDefinition) {
        return new TaskSpreaderCommon(taskServer, actorDefinition, objectFactory);
    }
}