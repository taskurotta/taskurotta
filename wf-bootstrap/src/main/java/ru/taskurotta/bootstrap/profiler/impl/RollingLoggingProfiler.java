package ru.taskurotta.bootstrap.profiler.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

public class RollingLoggingProfiler implements Profiler {

    public static final String START = "start_";
    public static final String END = "end_";

    private Map<String, Date> taskMeterMap = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(RollingLoggingProfiler.class);

    private String name = "";

    public void setName(String name) {
        this.name = name;
    }

    public RollingLoggingProfiler(long sleep) {
        if (sleep > 0) {
            runMonitor(sleep);
        }
    }

    public void logResult() {

        if (taskMeterMap != null && !taskMeterMap.isEmpty()) {
            StringBuilder sb = new StringBuilder("\n");
            Set<String> taskIdSet = new HashSet<>();
            for (String key : taskMeterMap.keySet()) {
                taskIdSet.add(extractId(key));
            }

            int i = 0;
            long minStart = -1l;
            long maxEnd = -1l;
            for (String key : taskIdSet) {

                long start = taskMeterMap.get(START + key) != null ? taskMeterMap.get(START + key).getTime() : -1l;
                long end = taskMeterMap.get(END + key) != null ? taskMeterMap.get(END + key).getTime() : -1l;
                long delta = (start > 0 && end > 0) ? (end - start) : -1l;

                if (i++ == 0) {
                    minStart = start;
                    maxEnd = end;
                } else {
                    minStart = start < minStart ? start : minStart;
                    maxEnd = end > maxEnd ? end : maxEnd;
                }
                if (logger.isDebugEnabled()) {
                    sb.append(name + ": " + i + ". Task [" + key + "]: start[" + start + "]ms, end[" + end + "]ms, delta[" + delta + "]ms \n");
                }

            }

            long totalDelta = maxEnd - minStart;
            long rate = taskIdSet.size() * 1000 / totalDelta;
            logger.info(name + ": " + sb.toString() + "TOTAL: maxEnd: [" + maxEnd + "]ms, minStart[" + minStart + "]ms, delta[" + totalDelta + "]ms, tasks[" + taskIdSet.size() + "], rate[" + rate + "]tasks/sec");

        }
    }


    private static String extractId(String key) {
        return key.replaceAll("^" + START, "").replaceAll("^" + END, "");
    }


    /**
     * Периодически будет выводить в лог итоговые данные по задачам
     *
     * @param sleep
     */
    private void runMonitor(final long sleep) {
        Thread monitor = new Thread() {

            @Override
            public void run() {
                while (true) {
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

    @Override
    public RuntimeProcessor decorate(RuntimeProcessor runtimeProcessor) {
        return runtimeProcessor;
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {

            @Override
            public Task poll() {
                Date date = new Date();
                Task result = taskSpreader.poll();
                if (result != null) {
                    taskMeterMap.put(START + result.getId().toString(), date);
                }
                return result;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
                taskMeterMap.put(END + taskDecision.getId().toString(), new Date());
            }

        };
    }

    @Override
    public void cycleStart() {
    }

    @Override
    public void cycleFinish() {
    }


}
