package ru.taskurotta.backend.queue;

import ru.taskurotta.backend.queue.model.QueuedItem;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:12 PM
 */
public class QueueBackend {

    public QueuedItem poll(ActorDefinition actorDefinition) {
        return null;
    }

    public void enqueueItem(ActorDefinition actorDefinition, QueuedItem queuedItem) {
    }
}
