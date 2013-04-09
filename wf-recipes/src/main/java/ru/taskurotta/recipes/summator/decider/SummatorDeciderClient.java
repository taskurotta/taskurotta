package ru.taskurotta.recipes.summator.decider;

import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;

import java.util.List;

/**
 * Created by void 05.04.13 18:57
 */
@DeciderClient(decider = SummatorDecider.class)
public interface SummatorDeciderClient {

	@Execute
	public void start(List<Integer> data);
}
