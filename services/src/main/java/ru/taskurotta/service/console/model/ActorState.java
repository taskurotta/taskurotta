package ru.taskurotta.service.console.model;

/**
 */
public enum ActorState {
    
    ACTIVE(0), INACTIVE(1), BLOCKED(2);

    int value;

    ActorState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }


    public static ActorState fromInt(int i) {
        if (i == 0) return ActorState.ACTIVE;
        if (i == 1) return ActorState.INACTIVE;
        if (i == 2) return ActorState.BLOCKED;
        return null;
    }

}