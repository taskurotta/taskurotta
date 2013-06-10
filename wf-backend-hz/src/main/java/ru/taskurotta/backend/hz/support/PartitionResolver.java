package ru.taskurotta.backend.hz.support;

import java.util.UUID;

/**
 * User: dimadin
 * Date: 10.06.13 13:27
 */
public interface PartitionResolver {

    public Object resolveByUUID(UUID uuid);

}
