package ru.taskurotta.client.internal;

import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:30
 */
public class TaskSpreaderProviderCommon implements TaskSpreaderProvider {

    private TaskServer taskServer;

    public TaskSpreaderProviderCommon(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

    @Override
    public TaskSpreader getTaskSpreader(ActorDefinition actorDefinition) {
        return new TaskSpreaderCommon(taskServer, actorDefinition);
    }
}