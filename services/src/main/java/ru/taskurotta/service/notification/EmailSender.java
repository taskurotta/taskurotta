package ru.taskurotta.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import ru.taskurotta.service.notification.model.EmailAttach;
import ru.taskurotta.service.notification.model.Notification;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;

/**
 * Created on 08.06.2015.
 */
public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private JavaMailSender mailSender;

    public void send(Notification notification) {
        try {
            mailSender.send(toMimeMessage(notification));
            logger.info("Notification [{}] successfully sent", notification);
        } catch (Exception e) {
            logger.error("Cannot send email notification ["+notification+"]", e);
        }
    }

    private MimeMessage toMimeMessage(final Notification notification) throws Exception {

        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws javax.mail.MessagingException {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage, notification.isMultipart(), notification.getEncoding());
                message.setFrom(notification.getSendFrom());
                message.setTo(notification.getSendTo());
                message.setSubject(notification.getSubject());
                message.setText(notification.getBody(), notification.isHtml());

                addAttaches(message, notification.getAttaches());
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
