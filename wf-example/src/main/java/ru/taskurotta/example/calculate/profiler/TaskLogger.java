package ru.taskurotta.example.calculate.profiler;

import java.util.Date;

public interface TaskLogger {
	
	/**
	 * Печатает влог мгновенную статистику по выполненным задачам
	 */
	public void logResult();
	
	/**
	 * Отмечает, что задача с данным ID начала выполнеение в заданное время 
	 */
	public void markTaskStart(String taskId, Date date);
	
	/**
	 * Отмечает, что задача с данным ID завершилда выполнение в заданное время 
	 */
	public void markTaskEnd(String taskId, Date date);

}
