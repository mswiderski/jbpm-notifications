package org.jbpm.extensions.notifications.api;

import org.jbpm.extensions.notifications.api.service.RecipientService;

public interface ReceivedMessageCallback {

	void onMessage(RecipientService recipientService, Message message);
}
