package ru.taskurotta.util;

import org.springframework.util.StringUtils;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.exception.ActorRuntimeException;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 9:06 PM
 */
public class ActorDefinition {

    private String name;

    private String version;
    
    private ActorDefinition() {
    }    
    
    public static ActorDefinition valueOf(String name, String version) {
    	ActorDefinition actorDefinition = new ActorDefinition();
    	actorDefinition.name = name;
    	actorDefinition.version = version;
    	return actorDefinition;
    }
    
    public static ActorDefinition valueOf(Class actorClass) {

        Class<?> workerInterface = AnnotationUtils.findAnnotatedClass(actorClass, Worker.class);

        if (workerInterface != null) {

            ActorDefinition actorDefinition = new ActorDefinition();

            Worker workerAnnotation = workerInterface.getAnnotation(Worker.class);

            actorDefinition.name = workerAnnotation.name();
            actorDefinition.version = workerAnnotation.version();

            if (!StringUtils.hasText(actorDefinition.name)) {
                actorDefinition.name = workerInterface.getName();
            }

            return actorDefinition;
        }


        Class<?> deciderInterface = AnnotationUtils.findAnnotatedClass(actorClass, Decider.class);

        if (deciderInterface != null) {

            ActorDefinition actorDefinition = new ActorDefinition();

            Decider deciderAnnotation = deciderInterface.getAnnotation(Decider.class);

            actorDefinition.name = deciderAnnotation.name();
            actorDefinition.version = deciderAnnotation.version();

            if (!StringUtils.hasText(actorDefinition.name)) {
                actorDefinition.name = deciderInterface.getName();
            }

            return actorDefinition;
        }

        throw new ActorRuntimeException("Class [" + actorClass.getName() + "] has no either annotation @Worker or @Decider");
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
