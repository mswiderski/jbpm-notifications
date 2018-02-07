package org.jbpm.extensions.notifications.impl.handlers;

import java.util.Map;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.ReceivedMessageHandler;
import org.jbpm.runtime.manager.impl.identity.UserDataServiceProvider;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.internal.task.api.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartProcessHandler implements ReceivedMessageHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(StartProcessHandler.class);

	private UserInfo userInfo;
    
    public StartProcessHandler() {
        this.userInfo = UserDataServiceProvider.getUserInfo();
    }
	
	@Override
	public void onMessage(Message message) {
		
		String messageId = message.getMessageId();// refers to In-Reply-To
		logger.debug("Received message with id {} is {}", messageId, message);
		
		if (!messageId.trim().isEmpty()) {
			logger.debug("Skiping start process callback as the message is a reply");
		    return;
		}
		String containerId;
        String processId;
		
		String[] subjectSplit = message.getSubject().split(":");
			
		if (subjectSplit.length != 2) {
		    logger.debug("Subject " + message.getSubject() + " has invalid format, quiting");
		    return;
		}
		containerId = subjectSplit[0];
        processId = subjectSplit[1];
		
		String userId = userInfo.getEntityForEmail(message.getSender());
		Map<String, Object> parameters = message.getData();
		parameters.put("sender", userId);
		
		ProcessService processService = (ProcessService) ServiceRegistry.get().service(ServiceRegistry.PROCESS_SERVICE);
		logger.debug("About to start process with id {} with data {} in container {}", processId, parameters, containerId);
		long processInstanceId = processService.startProcess(containerId, processId, parameters);
		logger.debug("Process instance started with id {} for message {}", processInstanceId, message);
	}

}
