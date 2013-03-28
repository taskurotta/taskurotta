package ru.taskurotta.core;

/**
 * Created by void 22.03.13 13:27
 */
public enum ArgType {
	NONE (0), WAIT(1), NO_WAIT(2);

	private final int value;

	private ArgType(int value) {
		this.value = value;
	}

	public static ArgType fromInt(int i) {
        if (i == 0) return ArgType.NONE;
		if (i == 1) return ArgType.WAIT;
		if (i == 2) return ArgType.NO_WAIT;
		return null;
	}

	public int getValue() {
		return value;
	}
}
