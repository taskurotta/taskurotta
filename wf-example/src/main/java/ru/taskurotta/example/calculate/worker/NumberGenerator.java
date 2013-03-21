package ru.taskurotta.example.calculate.worker;

import ru.taskurotta.annotation.Worker;

@Worker
public interface NumberGenerator {
	
	public Integer getNumber();
	
}
