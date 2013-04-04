package ru.taskurotta.backend.config.model;

import java.util.Properties;

import ru.taskurotta.util.ActorDefinition;

public class ActorPreferences {
	
	private String name;
	private String version;
	private boolean blocked = false;
	private ExpirationPolicyConfig expirationPolicy;
	
	public ActorDefinition getActorDefinition() {
		return ActorDefinition.valueOf(name, version);
	}
	public boolean isBlocked() {
		return blocked;
	}
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
	public ExpirationPolicyConfig getExpirationPolicy() {
		return expirationPolicy;
	}
	public void setExpirationPolicy(ExpirationPolicyConfig expirationPolicy) {
		this.expirationPolicy = expirationPolicy;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
			return "ExpirationPolicyConfig [className=" + className
					+ ", properties=" + properties + "]";
		}
	}
	
	@Override
	public String toString() {
		return "ActorPreferences [name=" + name + ", version=" + version
				+ ", blocked=" + blocked + ", expirationPolicy="
				+ expirationPolicy + "]";
	}		

}
