package ru.taskurotta.internal.proxy;

import ru.taskurotta.annotation.Worker;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.transport.model.TaskType;

import java.lang.annotation.Annotation;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 9:14 PM
 */
public class WorkerProxyFactory extends ActorProxyFactory {

    private static AnnotationExplorer workerAnnotationExplorer = new AnnotationExplorer() {

        @Override
        public Class getActorInterface(Annotation clientAnnotation) {
            return ((WorkerClient) clientAnnotation).worker();
        }

        @Override
        public String getActorName(Annotation actorAnnotation) {
            return ((Worker) actorAnnotation).name();
        }

        @Override
        public String getActorVersion(Annotation actorAnnotation) {
            return ((Worker) actorAnnotation).version();
        }

        @Override
        public TaskType getTaskType() {
            return TaskType.WORKER;
        }
    };


    public WorkerProxyFactory() {
        super(Worker.class, WorkerClient.class, workerAnnotationExplorer);
    }
}
