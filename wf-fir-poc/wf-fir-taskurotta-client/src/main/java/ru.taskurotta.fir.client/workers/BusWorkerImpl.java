package ru.taskurotta.fir.client.workers;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * User: stukushin
 * Date: 23.04.13
 * Time: 11:07
 */
public class BusWorkerImpl implements BusWorker {

    private JmsTemplate jmsTemplate;

    @Override
    public void sendPackage(final String uuid, final int result) {
        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage message = session.createTextMessage();
                message.setText(String.valueOf(result));
                message.setStringProperty("FCCNumber", uuid);
                return message;
            }
        });
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }
}
