package org.jbpm.extensions.notifications.api;

public interface ReceivedMessageHandler {

	void onMessage(Message message);
}
