package org.jbpm.extensions.notifications.api.service;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.ReceivedMessageHandler;

public interface NotificationService {

	void send(Message message);
	
	void start(ReceivedMessageHandler... callback);
	
	void stop();
}
