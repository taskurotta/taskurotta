package ru.taskurotta.recipes.summator.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

import java.util.List;

/**
 * Created by void 05.04.13 18:55
 */
@Decider
public interface SummatorDecider {

	@Execute
	public void start(List<Integer> data);
}
