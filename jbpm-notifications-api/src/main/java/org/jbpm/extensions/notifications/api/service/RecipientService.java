package org.jbpm.extensions.notifications.api.service;

public interface RecipientService {

	String getAddress(String userId);
	
	String getUserId(String address);
}
