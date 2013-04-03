package ru.taskurotta.test;

import java.util.HashSet;

/**
 * Created by void 03.04.13 17:37
 */
public class Stage {
	private final HashSet<String> tags;

	public Stage(String source) {
		if (source.contains(",")) {
			String[] splitted = source.split(",");
			tags = new HashSet<String>(splitted.length);
			for (String tag : splitted) {
				tags.add(tag.trim());
			}
		} else {
			tags = new HashSet<String>(1);
			tags.add(source);
		}
	}

	public boolean contains(String tag) {
		return tags.contains(tag);
	}

	public boolean isEmpty() {
		return tags.isEmpty();
	}

	public int size() {
		return tags.size();
	}

	/**
	 * Removes specifyed tag from this stage
	 * @param tag - tag to be removed
	 * @return true if stage is not empty
	 */
	public boolean remove(String tag) {
		return tags.remove(tag);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Stage stage = (Stage) o;

		if (!tags.equals(stage.tags)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return tags.hashCode();
	}

	@Override
	public String toString() {
		return "{" + tags + '}';
	}
}
