package org.jbpm.extensions.notifications.api.service;

import org.jbpm.extensions.notifications.api.Message;

public interface MessageTemplateService {

	String apply(Message message);
	
	void registerTemplate(String id, Object template);
	
	void unregisterTemplate(String id);
	
	String getResourceType();
}
