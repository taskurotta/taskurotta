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


    /**
     * get cached worker client proxy object
     *
     * @param type - interface of required object
     * @return proxy object or null
     */
    public <WorkerClientType> WorkerClientType getWorkerClient(Class<WorkerClientType> type);


    /**
     * get cached decider client proxy object
     *
     * @param type - interface of required object
     * @return proxy object or null
     */
    public <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type);


    /**
     * get cached decider proxy object
     *
     * @param type          - interface of required object
     * @param <AsynchronousClientType> - class of actor bean
     * @return proxy object or null
     */
    public <AsynchronousClientType> AsynchronousClientType getAsynchronousClient(Class<AsynchronousClientType> type);
}
