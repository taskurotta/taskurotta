package ru.taskurotta.example.calculate.decider;

import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;

@Decider
public interface MathActionDecider {
	
	@Execute
	public void performAction();
	
}
