package ru.taskurotta.example.calculate.profiler;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Записывает данные о времени начала и завершения задач в ConcurrentHashMap<String, Date>
 * Если в конструкторе передано число >0, то периодически (с периодом = этому числу мс) распечатывает в лог статистику.
 * В режиме INFO только суммарную, в DEBUG дополнительно по каждой задаче.  
 */
public class TaskLoggerImpl implements TaskLogger {
	
	public static final String START = "start_";
	public static final String END = "end_";
	
	private Map<String, Date> taskMeterMap = new ConcurrentHashMap<String, Date>();
	
	private static final Logger logger = LoggerFactory.getLogger(TaskLoggerImpl.class);
	
	public void markTaskStart(String taskId, Date date) {
		taskMeterMap.put(START+taskId, date);
	}
	
	public void markTaskEnd(String taskId, Date date) {
		taskMeterMap.put(END+taskId, date);
	}
	
	
	public TaskLoggerImpl(long sleep) {
		if(sleep>0) {
			runMonitor(sleep);
		}
	}

	public TaskLoggerImpl() {
		this(-1l);
	}
	
	public void logResult() {
		
		if(taskMeterMap!=null && !taskMeterMap.isEmpty()) {
			StringBuilder sb = new StringBuilder("\n");
			Set<String> taskIdSet = new HashSet<String>();
			for(String key: taskMeterMap.keySet()) {
				taskIdSet.add(extractId(key));
			}
			
			int i = 0;
			long minStart = -1l;
			long maxEnd = -1l;
			for(String key : taskIdSet) {
				
				long start = taskMeterMap.get(START+key)!=null? taskMeterMap.get(START+key).getTime(): -1l;
				long end = taskMeterMap.get(END+key)!=null? taskMeterMap.get(END+key).getTime(): -1l;
				long delta = (start>0&&end>0)? (end-start): -1l;

				if(i++ == 0) {
					minStart = start;
					maxEnd = end;
				} else {
					minStart = start<minStart? start: minStart;
					maxEnd = end>maxEnd? end: maxEnd;
				}
				if(logger.isDebugEnabled()) {
					sb.append(i + ". Task ["+key+"]: start["+start+"]ms, end["+end+"]ms, delta["+delta+"]ms \n");	
				}
				
			}
			
			long totalDelta = maxEnd-minStart;
			long rate = taskIdSet.size()*1000/totalDelta;
			logger.info(sb.toString() + "TOTAL: maxEnd: ["+maxEnd+"]ms, minStart["+minStart+"]ms, delta["+totalDelta+"]ms, tasks["+taskIdSet.size()+"], rate["+rate+"]tasks/sec");
			
		}
	}
	
	
	public static String extractId(String key) {
		return key.replaceAll("^"+START, "").replaceAll("^"+END, "");
	}
	
	
	/**
	 * Периодически будет выводить в лог итоговые данные по задачам
	 * @param sleep
	 */
	public void runMonitor(final long sleep) {
        Thread monitor = new Thread() {

			@Override
			public void run() {
		        while(true) {
		        	try {
						Thread.sleep(sleep);
						logResult();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		        }
			}
        	
        };
        monitor.setDaemon(true);
        monitor.start();
	}

}
