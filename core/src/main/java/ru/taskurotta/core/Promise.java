package ru.taskurotta.core;

import java.util.UUID;

/**
 * This class represents result of asynchronous method invocation. It is used for maintain non-blocking links between
 * method invocations.
 * Created by jedy 05.12.12 13:08
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

    /**
     * check if this promise contains result value already
     * @return true if promise fully initialized
     */
    public boolean isReady() {
		return isReady;
	}

    /**
     * check if this promise contains Fail
     * @return true if this promise contains Fail
     */
    public boolean hasFail() {
        return fail != null;
    }

    public Fail getFail() {
        return fail;
    }

    public void setFail(Fail fail) {
        this.fail = fail;
    }

    /**
     *
     * @return result of asynchronous method invocation
     * @throws java.lang.IllegalStateException if promise isn't ready yet
     * @throws Fail if asynchronous method fails with Exception
     */
    public T get() {
		if (!isReady()) {
			throw new IllegalStateException("Promise (" + id + ") isn't ready");
		}

        if (hasFail()) {
            fail.fillInStackTrace();
            throw fail;
        }

        return value;
	}

	public Promise<T> set(T value) {
        isReady = true;
		this.value = value;
		return this;
	}

    /**
     * construct new and ready Promise object
     * @param value - value for promise
     * @param <E> - type of the value
     * @return new Promise object
     */
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

        return id == null ? promise.id == null : id.equals(promise.id);
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
