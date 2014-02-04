package ru.taskurotta.util;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.exception.ActorRuntimeException;

import java.io.Serializable;

/**
 *
 * User: romario
 * Date: 2/12/13
 * Time: 9:06 PM
 */
public class ActorDefinition implements Serializable {

    private String name;

    private String version;

    private String taskList;

    private String fullName;
    
    private ActorDefinition() {}
    
    public static ActorDefinition valueOf(String name, String version) {
    	ActorDefinition actorDefinition = new ActorDefinition();
    	actorDefinition.name = name;
    	actorDefinition.version = version;
    	return actorDefinition;
    }

    public static ActorDefinition valueOf(String name, String version, String taskList) {
        ActorDefinition actorDefinition = new ActorDefinition();
        actorDefinition.name = name;
        actorDefinition.version = version;
        actorDefinition.taskList = taskList;
        return actorDefinition;
    }
    
    public static ActorDefinition valueOf(Class actorClass) {

        Class<?> workerInterface = AnnotationUtils.findAnnotatedClass(actorClass, Worker.class);

        if (workerInterface != null) {

            ActorDefinition actorDefinition = new ActorDefinition();

            Worker workerAnnotation = workerInterface.getAnnotation(Worker.class);

            actorDefinition.name = workerAnnotation.name();
            actorDefinition.version = workerAnnotation.version();

            if (StringUtils.isBlank(actorDefinition.name)) {
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

            if (StringUtils.isBlank(actorDefinition.name)) {
                actorDefinition.name = deciderInterface.getName();
            }

            return actorDefinition;
        }

        throw new ActorRuntimeException("Class [" + actorClass.getName() + "] has no either annotation @Worker or @Decider");
    }

    public static ActorDefinition valueOf(Class actorClass, String taskList) {
        ActorDefinition actorDefinition = valueOf(actorClass);
        actorDefinition.taskList = taskList;

        return actorDefinition;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getTaskList() {
        return taskList;
    }

    public String getFullName() {

        if (fullName != null) {
            return fullName;
        }

        return fullName = name + "#" + version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActorDefinition that = (ActorDefinition) o;

        if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (taskList != null ? !taskList.equals(that.taskList) : that.taskList != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (taskList != null ? taskList.hashCode() : 0);
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActorDefinition{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", taskList='" + taskList + '\'' +
                ", fullName='" + fullName + '\'' +
                "} " + super.toString();
    }
}
