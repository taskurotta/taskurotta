package ru.taskurotta.test.flow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by void 03.04.13 17:37
 */
public class Stage {
	private final List<String> tagList;

	public Stage(String source) {
		if (source.contains(",")) {
			String[] splitted = source.split(",");
            tagList = new ArrayList<String>(splitted.length);
            for (String tag : splitted) {
                tagList.add(tag.trim());
            }

		} else {
            tagList = new ArrayList<String>(1);
            tagList.add(source);
		}
	}

	public boolean contains(String tag) {
		return tagList.contains(tag);
	}

	public boolean isEmpty() {
		return tagList.isEmpty();
	}

	public int size() {
		return tagList.size();
	}

	/**
	 * Removes specifyed tag from this stage
	 * @param tag - tag to be removed
	 * @return true if stage is not empty
	 */
	public boolean remove(String tag) {
		return tagList.remove(tag);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Stage stage = (Stage) o;

		if (!tagList.equals(stage.tagList)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return tagList.hashCode();
	}

	@Override
	public String toString() {
		return "{" + tagList + '}';
	}
}
