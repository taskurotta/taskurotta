package ru.taskurotta.restarter.workers;

import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 16:50
 */

@WorkerClient(worker = Analyzer.class)
public interface AnalyzerClient {
    public Promise<List<TaskContainer>> findNotFinishedProcesses(long fromTime);
}
