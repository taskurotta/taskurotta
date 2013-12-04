package ru.taskurotta.core;

import java.util.UUID;

/**
 * User: jedy
 * Date: 05.12.12 13:08
 */
public class Promise<T> {

	private UUID id;
	private T value;

    private boolean isReady = false;
    private Fail fail;

	public Promise() {
		id = UUID.randomUUID();
	}

    private Promise(UUID uuid, T value) {
        this.id = uuid;
        this.value = value;
    }

    public static Promise createInstance(UUID uuid) {
        return new Promise(uuid, null);
    }

	public boolean isReady() {
		return isReady;
	}

    public boolean containsFail() {
        return fail != null;
    }

    public Fail getFail() {
        return fail;
    }

    public void setFail(Fail fail) {
        this.fail = fail;
    }

    public T get() {
		if (!isReady()) {
			throw new IllegalStateException("Promise (" + id + ") isn't ready");
		}
		return value;
	}

	public Promise<T> set(T value) {
        isReady = true;
		this.value = value;
		return this;
	}

	public static <E> Promise<E> asPromise(E value) {
		return new Promise<E>().set(value);
	}

	public UUID getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Promise)) return false;

		Promise promise = (Promise) o;

        return !(id != null ? !id.equals(promise.id) : promise.id != null);
    }

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Promise{" +
				"id=" + id +
				", isReady=" + isReady +
                ", value=" + value +
				'}';
	}
}
