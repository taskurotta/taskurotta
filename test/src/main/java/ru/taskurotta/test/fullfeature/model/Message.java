package ru.taskurotta.test.fullfeature.model;

/**
 * Created by void 26.12.13 19:32
 */
public class Message {
    private User recipient;
    private String message;

    public Message(User recipient, String message) {
        this.recipient = recipient;
        this.message = message;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
