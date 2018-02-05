package org.jbpm.extensions.notifications.impl.service;

import java.util.Arrays;
import java.util.Properties;

import org.jbpm.extensions.notifications.impl.MessageImpl;
import org.jbpm.extensions.notifications.impl.ServiceRepository;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class EmailNotificationServiceTest {

	
	@Test
	public void testReceiveMessage() throws Exception {
		
		DefaultRecipientService recipientService = new DefaultRecipientService();
		recipientService.add("maciek", "swiderski.maciej@gmail.com");
		ServiceRepository.get().addService("RecipientService", recipientService);
		
		TestReceivedMessagedCallback callback = new TestReceivedMessagedCallback();
		
		Properties emailServiceConfiguration = new Properties();
        emailServiceConfiguration.load(this.getClass().getResourceAsStream("/email-service.properties"));
		
		EmailNotificationService service = new EmailNotificationService(recipientService, emailServiceConfiguration);				
		
		String messageId = "jbpm/containers/test/tasks/3";
		MessageImpl message = new MessageImpl();
		message.setMessageId(messageId);
//		message.setData(task.getTaskData().getTaskInputVariables());
		message.setContent("Task is ready for you. Reply to this email to complete the task");
		message.setSubject("New task available");
		message.setRecipients(Arrays.asList("maciek"));
		
		service.send(message);
		service.start(callback);
		
		Thread.sleep(30000);
		
		service.stop();
	}
}
