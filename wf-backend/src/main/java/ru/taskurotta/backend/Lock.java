package ru.taskurotta.backend;

import java.util.Date;

public class Lock {
	
	private Date expires;
	private String lockerId;
	
	public Lock(String lockerId, Date expires) {
		this.lockerId = lockerId;
		this.expires = expires;
	}
	
	public boolean isExpired() {
		return new Date().after(expires);
	}
	
	public Date getExpires() {
		return expires;
	}
	public String getLockerId() {
		return lockerId;
	}

	@Override
	public String toString() {
		return "Lock [expires=" + expires + ", lockerId=" + lockerId + "]";
	}

}
