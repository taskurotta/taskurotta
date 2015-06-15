package ru.taskurotta.service.notification.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created on 08.06.2015.
 */
public class EmailNotification implements Serializable {

    private String sendTo;
    private String subject;
    private String body;
    private boolean isHtml = true;
    private boolean isMultipart = true;
    private String encoding = "UTF-8";
    private List<EmailAttach> attaches;

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setIsHtml(boolean isHtml) {
        this.isHtml = isHtml;
    }

    public boolean isMultipart() {
        return isMultipart;
    }

    public void setIsMultipart(boolean isMultipart) {
        this.isMultipart = isMultipart;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public List<EmailAttach> getAttaches() {
        return attaches;
    }

    public void setAttaches(List<EmailAttach> attaches) {
        this.attaches = attaches;
    }

    @Override
    public String toString() {
        return "Notification{" +
                ", sendTo='" + sendTo + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", isHtml=" + isHtml +
                ", isMultipart=" + isMultipart +
                ", encoding='" + encoding + '\'' +
                ", attaches=" + attaches +
                '}';
    }
}
