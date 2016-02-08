package ru.taskurotta.test.stress;

import org.jetbrains.annotations.NotNull;
import ru.taskurotta.server.GeneralTaskServer;
import ru.taskurotta.service.hz.storage.StringSetCounter;

import java.util.List;

/**
 * Created on 16.02.2015.
 */
public class SameJVMStringSetCounter implements StringSetCounter {

    @Override
    public long getSize() {
        return GeneralTaskServer.finishedProcessesCounter.get();
    }

    @Override
    public void add(@NotNull String customId) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @NotNull
    @Override
    public List<String> findUniqueItems(@NotNull List<String> supposedUniqueList) {
        throw new IllegalStateException("Not implemented yet!");
    }
}
