package ru.fccland.wf.ws.usersession;

/**
 * Created by void 15.03.13 13:11
 */
public class SessionInfo {
    private final Long id;
    private final String message;

    public SessionInfo(Long id, String message) {
        this.id = id;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
