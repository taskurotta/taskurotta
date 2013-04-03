package ru.taskurotta.backend.config.model;

import java.util.Properties;

public class ActorPreferences {
	
	private String className;
	private String version;
	
	private ExpirationPolicyConfig expirationPolicy;
	
	public ExpirationPolicyConfig getExpirationPolicy() {
		return expirationPolicy;
	}
	public void setExpirationPolicy(ExpirationPolicyConfig expirationPolicy) {
		this.expirationPolicy = expirationPolicy;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	public class ExpirationPolicyConfig {
		
		private String className;
		private Properties properties;
		
		public String getClassName() {
			return className;
		}
		public void setClassName(String className) {
			this.className = className;
		}
		public Properties getProperties() {
			return properties;
		}
		public void setProperties(Properties properties) {
			this.properties = properties;
		}
		
		@Override
		public String toString() {
			return "ExpirationPolicy [className=" + className + ", properties="
					+ properties + "]";
		}
	}
	
	@Override
	public String toString() {
		return "ActorPreferences [className=" + className + ", version="
				+ version + ", expirationPolicy=" + expirationPolicy + "]";
	}		

}
