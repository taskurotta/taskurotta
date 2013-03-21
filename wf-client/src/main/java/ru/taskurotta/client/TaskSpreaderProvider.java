package ru.taskurotta.client;

import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 1/29/13
 * Time: 5:23 PM
 */
public interface TaskSpreaderProvider {

    public TaskSpreader getTaskSpreader(ActorDefinition actorDefinition);

}
