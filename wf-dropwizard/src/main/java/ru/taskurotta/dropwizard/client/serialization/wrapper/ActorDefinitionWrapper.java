package ru.taskurotta.dropwizard.client.serialization.wrapper;

import ru.taskurotta.dropwizard.client.serialization.ActorDefinitionDeserializer;
import ru.taskurotta.dropwizard.client.serialization.ActorDefinitionSerializer;
import ru.taskurotta.util.ActorDefinition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ActorDefinitionWrapper {

    private ActorDefinition actorDefinition;

    public ActorDefinitionWrapper() {
    }

    public ActorDefinitionWrapper(ActorDefinition actorDefinition) {
        this.actorDefinition = actorDefinition;
    }

    @JsonSerialize(using=ActorDefinitionSerializer.class)
    public ActorDefinition getActorDefinition() {
        return actorDefinition;
    }

    @JsonDeserialize(using=ActorDefinitionDeserializer.class)
    public void setActorDefinition(ActorDefinition actorDefinition) {
        this.actorDefinition = actorDefinition;
    }

    @Override
    public String toString() {
        return "ActorDefinitionWrapper [name=" + (actorDefinition!=null? actorDefinition.getName(): null) + ", version = " + (actorDefinition!=null? actorDefinition.getVersion(): null)  + "]";
    }

}
