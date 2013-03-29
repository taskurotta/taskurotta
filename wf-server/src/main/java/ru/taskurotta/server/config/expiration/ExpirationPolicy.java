package ru.taskurotta.server.config.expiration;

import ru.taskurotta.server.model.TaskObject;

/**
 * Класс, описывающий политику сервера в отношении задач, 
 * которые не получали конечного статуса за отведённое время
 *
 */
public interface ExpirationPolicy {
	
	public boolean isScheduleAgain(TaskObject task);
	
	public String getPolicyName();
	
}
