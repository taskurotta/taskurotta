package ru.taskurotta.restarter.workers;

import ru.taskurotta.annotation.Worker;
import ru.taskurotta.restarter.ProcessVO;

import java.util.List;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 16:44
 */
@Worker
public interface Analyzer {
    public List<ProcessVO> findNotFinishedProcesses(long fromTime);
}
