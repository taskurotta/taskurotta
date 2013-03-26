package ru.taskurotta.example.calculate.worker;

import ru.taskurotta.annotation.Worker;

@Worker
public interface Multiplier {
	
	public Integer multiply(Integer a, Integer b);
	
}
