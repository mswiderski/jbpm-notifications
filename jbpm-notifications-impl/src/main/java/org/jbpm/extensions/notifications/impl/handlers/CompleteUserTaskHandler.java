package org.jbpm.extensions.notifications.impl.handlers;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.ReceivedMessageHandler;
import org.jbpm.runtime.manager.impl.identity.UserDataServiceProvider;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.internal.task.api.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompleteUserTaskHandler implements ReceivedMessageHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(CompleteUserTaskHandler.class);
	
	private UserInfo userInfo;
		
    public CompleteUserTaskHandler() {
        this.userInfo = UserDataServiceProvider.getUserInfo();
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
		if (messageIdElements.length < 5 || !messageIdElements[3].equals("tasks")) {
			logger.debug("Message ID " + messageId + " has invalid format, skipping user task completion");
			return;
		}
		String containerId = messageIdElements[2];
		String taskId = messageIdElements[4];
		logger.debug("Message refers to container {} and task {}", containerId, taskId);
		
		UserTaskService userTaskService = (UserTaskService) ServiceRegistry.get().service(ServiceRegistry.USER_TASK_SERVICE);
		String userId = userInfo.getEntityForEmail(message.getSender());
		logger.debug("About to complete task {} with data {} as user {}", taskId, message.getData(), userId);
		userTaskService.completeAutoProgress(Long.valueOf(taskId), userId, message.getData());

	}

}
