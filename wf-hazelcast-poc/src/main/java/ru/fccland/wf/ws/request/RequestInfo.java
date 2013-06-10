package ru.fccland.wf.ws.request;

/**
 * Created by void 15.03.13 18:56
 */
public class RequestInfo {
	private final Long id;
	private final String user;
	private final String data;

	public RequestInfo(Long id, String user, String data) {
		this.id = id;
		this.user = user;
		this.data = data;
	}

	public Long getId() {
		return id;
	}

	public String getUser() {
		return user;
	}

	public String getData() {
		return data;
	}
}
