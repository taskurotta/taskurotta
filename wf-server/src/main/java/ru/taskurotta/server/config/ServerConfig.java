package ru.taskurotta.server.config;

import java.util.Arrays;


/**
 * Configuration object for a given TaskServer instance
 */
public class ServerConfig {

    private String location;//local, URL, externalFile

    private ActorConfig[] actorConfigs;

    public ActorConfig[] getActorConfigs() {
        return actorConfigs;
    }

    public void setActorConfigs(ActorConfig[] actorConfigs) {
        this.actorConfigs = actorConfigs;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "ServerConfig [location=" + location + ", actorConfigs="
                + Arrays.toString(actorConfigs) + "]";
    }

}
