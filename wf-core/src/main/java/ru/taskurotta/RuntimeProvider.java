package ru.taskurotta;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 4:18 PM
 */
public interface RuntimeProvider {

    /**
     * @param actorBean implements @Decider or @Worker annotated interface
     * @return task runtime processor
     */
    public RuntimeProcessor getRuntimeProcessor(Object actorBean);

}
