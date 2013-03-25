package ru.taskurotta;

import ru.taskurotta.internal.GeneralRuntimeProvider;

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

        return new GeneralRuntimeProvider();
    }

}
