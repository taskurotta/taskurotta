package ru.taskurotta.internal.proxy;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.util.StringUtils;

import java.lang.annotation.Annotation;

/**
 * created by void 22.01.13 19:45
 */
public class DeciderProxyFactory extends ActorProxyFactory {

    private static AnnotationExplorer deciderAnnotationExplorer = new AnnotationExplorer() {

        @Override
        public Class getActorInterface(Annotation clientAnnotation) {
            return ((DeciderClient) clientAnnotation).decider();
        }

        @Override
        public String getActorName(Annotation actorAnnotation) {
            return ((Decider) actorAnnotation).name();
        }

        @Override
        public String getActorVersion(Annotation actorAnnotation) {
            return ((Decider) actorAnnotation).version();
        }

        @Override
        public TaskType getTaskType() {
            return TaskType.DECIDER_START;
        }
    };


    public DeciderProxyFactory() {
        super(Decider.class, DeciderClient.class, deciderAnnotationExplorer);
    }

    public static String deciderName(Class<?> deciderInterface) {
        Decider deciderAnnotation = deciderInterface.getAnnotation(Decider.class);

        String deciderName = deciderAnnotation.name();
        if (StringUtils.isBlank(deciderName)) {
            deciderName = deciderInterface.getName();
        }

        return deciderName;
    }

    public static String deciderVersion(Class<?> deciderInterface) {
        return deciderInterface.getAnnotation(Decider.class).version();
    }

}
