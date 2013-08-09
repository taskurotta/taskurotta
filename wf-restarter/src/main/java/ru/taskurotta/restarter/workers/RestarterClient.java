package ru.taskurotta.restarter.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.restarter.ProcessVO;

import java.util.List;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:20
 */
@WorkerClient(worker = Restarter.class)
public interface RestarterClient {
    public void restart(List<ProcessVO> processes);
}
