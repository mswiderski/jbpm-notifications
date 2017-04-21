package org.jbpm.extensions.notifications.impl.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jbpm.extensions.notifications.api.service.MessageTemplateService;
import org.jbpm.extensions.notifications.api.service.NotificationService;
import org.jbpm.extensions.notifications.impl.MessageImpl;
import org.jbpm.extensions.notifications.impl.ServiceRepository;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.UserInfo;
import org.kie.internal.task.api.model.InternalTask;

public class NotificationTaskEventListener implements TaskLifeCycleEventListener {

	private NotificationService notificationService = (NotificationService) ServiceRepository.get().getService("EmailService");
	private MessageTemplateService templateService = (MessageTemplateService) ServiceRepository.get().getService("TemplateService");
	
	@Override
	public void beforeTaskActivatedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskClaimedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskSkippedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskStartedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskStoppedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskCompletedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskFailedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskAddedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskExitedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskReleasedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskResumedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskSuspendedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskForwardedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskDelegatedEvent(TaskEvent event) {
		

	}

	@Override
	public void beforeTaskNominatedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskActivatedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskClaimedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskSkippedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskStartedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskStoppedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskCompletedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskFailedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskAddedEvent(TaskEvent event) {
		
		if (this.notificationService == null) {
			return;
		}
		UserInfo userInfo = (UserInfo) ((TaskContext)event.getTaskContext()).get(EnvironmentName.TASK_USER_INFO);
		
		Task task = event.getTask();
		String messageId = "jbpm/containers/" + task.getTaskData().getDeploymentId() + "/tasks/" + task.getId();
		MessageImpl message = new MessageImpl();
		message.setMessageId(messageId);
		message.setTemplate(((InternalTask)task).getFormName());
		
		Map<String, Object> data = new HashMap<>();
		data.put("task", task);
		if (task.getTaskData().getTaskInputVariables() != null) {
			data.putAll(task.getTaskData().getTaskInputVariables());
		}
		message.setData(data);		
		message.setSubject("New task available: " + task.getName() + " (" + task.getId() + ")");
		
		List<String> recipients = new ArrayList<>();
		recipients.addAll(task.getPeopleAssignments().getPotentialOwners()
				.stream()
				.filter(oe -> oe instanceof User)
				.map(entity -> entity.getId())
				.collect(Collectors.toList()));
		
		task.getPeopleAssignments().getPotentialOwners()
		.stream()
		.filter(oe -> oe instanceof Group)
		.forEach(group -> {
			Iterator<OrganizationalEntity> members = userInfo.getMembersForGroup((Group) group);
			while (members.hasNext()) {
				recipients.add(members.next().getId());
			}
		});
		message.setRecipients(recipients);
		message.setContent(templateService.apply(message));
		
		notificationService.send(message);
	}

	@Override
	public void afterTaskExitedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskReleasedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskResumedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskSuspendedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskForwardedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskDelegatedEvent(TaskEvent event) {
		

	}

	@Override
	public void afterTaskNominatedEvent(TaskEvent event) {
		

	}

}
