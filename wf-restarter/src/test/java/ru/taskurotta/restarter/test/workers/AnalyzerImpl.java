package ru.taskurotta.restarter.test.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.restarter.ProcessVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 16:51
 */
public class AnalyzerImpl implements Analyzer {

    private static Logger logger = LoggerFactory.getLogger(AnalyzerImpl.class);

    private static int counter = 0;

    @Override
    public List<ProcessVO> findNotFinishedProcesses(long fromTime) {
        logger.info("Try to find incomplete processes, was started before [{}] ({})", fromTime, new Date(fromTime));

        List<ProcessVO> processes = new ArrayList<>();

        if (counter++ < 1) {
            processes.add(new ProcessVO(UUID.randomUUID(), System.currentTimeMillis(), UUID.randomUUID(), ""));
        }

        logger.info("Found [{}] incomplete processes", processes.size());

        return processes;
    }

}
