package ru.taskurotta.service.hz.notification;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.service.notification.NotificationHandlersProvider;
import ru.taskurotta.service.notification.handler.TriggerHandler;

import java.util.Collection;
import java.util.Map;

/**
 * Created on 16.06.2015.
 */
public class SpringNotificationHandlersProvider implements NotificationHandlersProvider, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Collection<TriggerHandler> getHandlers() {
        Collection<TriggerHandler> result = null;
        Map<String, TriggerHandler> resultMap = applicationContext.getBeansOfType(TriggerHandler.class);
        if (resultMap!=null && !resultMap.isEmpty()) {
            result = resultMap.values();
        }
        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
