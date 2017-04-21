package org.jbpm.extensions.notifications.impl.callbacks;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.ReceivedMessageCallback;
import org.jbpm.extensions.notifications.api.service.RecipientService;
import org.jbpm.services.api.UserTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompleteUserTaskCallback implements ReceivedMessageCallback {
	
	private static final Logger logger = LoggerFactory.getLogger(CompleteUserTaskCallback.class);

	private UserTaskService userTaskService;
	private RecipientService recipientService;	
		
	public CompleteUserTaskCallback(UserTaskService userTaskService, RecipientService recipientService) {
		this.userTaskService = userTaskService;
		this.recipientService = recipientService;
	}


	@Override
	public void onMessage(Message message) {
		// expected message id format is org/containers/{containerid}/tasks/{id}@domain
		String messageId = message.getMessageId();
		logger.debug("Received message with id {} is {}", messageId, message);
		
		if (messageId.indexOf("@") != -1) {
			messageId = messageId.substring(0, messageId.indexOf("@"));
		}
		
		String[] messageIdElements = messageId.split("/");
		if (messageIdElements.length < 5) {
			throw new IllegalArgumentException("Message ID " + messageId + " has invalid format");
		}
		String containerId = messageIdElements[2];
		String taskId = messageIdElements[4];
		logger.debug("Message refers to container {} and task {}", containerId, taskId);
		
		String userId = recipientService.getUserId(message.getSender());
		logger.debug("About to complete task {} with data {} as user {}", taskId, message.getData(), userId);
		this.userTaskService.completeAutoProgress(Long.valueOf(taskId), userId, message.getData());

	}

}
