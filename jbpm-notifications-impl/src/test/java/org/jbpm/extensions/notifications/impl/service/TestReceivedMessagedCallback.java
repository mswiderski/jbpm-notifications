package org.jbpm.extensions.notifications.impl.service;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.ReceivedMessageHandler;

public class TestReceivedMessagedCallback implements ReceivedMessageHandler {

	private List<Message> receivedMessages = new ArrayList<>();
	
	@Override
	public void onMessage(Message message) {
		this.receivedMessages.add(message);

	}
	
	public List<Message> getMessages() {
		return this.receivedMessages;
	}
	
	public void clear() {
		this.receivedMessages.clear();
	}

}
