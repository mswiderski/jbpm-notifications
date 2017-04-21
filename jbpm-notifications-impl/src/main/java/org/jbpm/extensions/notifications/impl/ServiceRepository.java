package org.jbpm.extensions.notifications.impl;

import java.util.HashMap;
import java.util.Map;

public class ServiceRepository {
	
	private static ServiceRepository INSTANCE = new ServiceRepository();
	
	private Map<String, Object> services = new HashMap<>();
	
	public void addService(String name, Object service) {
		this.services.put(name, service);
	}
	
	public Object getService(String name) {
		return this.services.get(name);
	}
	
	public static ServiceRepository get() {
		return INSTANCE;
	}
}
