package ru.taskurotta.recipes.notification;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.notification.decider.NotificationDeciderClient;

/**
 * User: stukushin
 * Date: 15.02.13
 * Time: 14:27
 */
public class TaskCreator {

    private ClientServiceManager clientServiceManager;

    private long userId;
    private String message;
    private int count;

    public void createStartTask() {

        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        NotificationDeciderClient notificationDecider = deciderClientProvider.getDeciderClient(NotificationDeciderClient.class);

        for (int i = 0; i < count; i++) {
            notificationDecider.sendMessage(userId, message);
        }
    }

    public void setClientServiceManager(ClientServiceManager ClientServiceManager) {
        this.clientServiceManager = ClientServiceManager;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
