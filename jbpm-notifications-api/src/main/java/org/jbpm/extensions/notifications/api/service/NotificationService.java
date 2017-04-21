package org.jbpm.extensions.notifications.api.service;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.ReceivedMessageCallback;

public interface NotificationService {

	void send(Message message);
	
	void start(ReceivedMessageCallback... callback);
	
	void stop();
}
