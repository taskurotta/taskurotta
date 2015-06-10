package ru.taskurotta.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import ru.taskurotta.service.notification.model.EmailAttach;
import ru.taskurotta.service.notification.model.EmailNotification;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;

/**
 * Created on 08.06.2015.
 */
public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private JavaMailSender mailSender;

    public void send(EmailNotification emailNotification) {
        try {
            mailSender.send(toMimeMessage(emailNotification));
            logger.info("Notification [{}] successfully sent", emailNotification);
        } catch (Exception e) {
            logger.error("Cannot send email notification ["+ emailNotification +"]", e);
        }
    }

    private MimeMessage toMimeMessage(final EmailNotification emailNotification) throws Exception {

        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws javax.mail.MessagingException {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, emailNotification.isMultipart(), emailNotification.getEncoding());
                message.setFrom(emailNotification.getSendFrom());
                message.setTo(emailNotification.getSendTo());
                message.setSubject(emailNotification.getSubject());
                message.setText(emailNotification.getBody(), emailNotification.isHtml());

                addAttaches(message, emailNotification.getAttaches());
            }
        };

        MimeMessage result = mailSender.createMimeMessage();
        preparator.prepare(result);
        return result;
    }

    private void addAttaches(MimeMessageHelper message, List<EmailAttach> attaches) throws javax.mail.MessagingException {
        if (attaches!=null && !attaches.isEmpty()) {
            for (EmailAttach attach : attaches) {
                logger.debug("Adding email attach [{}]", attach);
                if (attach.getLocation() != null) {
                    message.addAttachment(attach.getName(), new File(attach.getLocation()));
                } else if (attach.getContent() != null) {
                    message.addAttachment(attach.getName(), new ByteArrayResource(attach.getContent()));
                }
            }
        }
    }

    @Required
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
}
