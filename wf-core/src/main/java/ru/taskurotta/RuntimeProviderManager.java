package ru.taskurotta;

import ru.taskurotta.internal.SWFRuntimeProvider;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 4:20 PM
 */
public final class RuntimeProviderManager {

    /**
     * @return default and so far only one provider implementation intern
     */
    public static RuntimeProvider getRuntimeProvider() {

        return new SWFRuntimeProvider();
    }

    /**
     * @param taskHandler
     * @return
     */
    public static RuntimeProvider getRuntimeProvider(TaskHandler taskHandler) {
       return  new SWFRuntimeProvider(taskHandler);
    }
}
