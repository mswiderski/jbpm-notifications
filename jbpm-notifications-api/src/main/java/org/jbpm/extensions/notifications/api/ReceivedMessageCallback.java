package org.jbpm.extensions.notifications.api;

public interface ReceivedMessageCallback {

	void onMessage(Message message);
}
