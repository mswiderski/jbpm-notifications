package org.jbpm.extensions.notifications.impl.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jbpm.extensions.notifications.api.service.NotificationService;
import org.jbpm.extensions.notifications.impl.MessageImpl;
import org.jbpm.process.workitem.email.TemplateManager;
import org.jbpm.services.api.service.ServiceRegistry;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.runtime.Cacheable;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.UserInfo;
import org.kie.internal.task.api.model.InternalTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationTaskEventListener implements TaskLifeCycleEventListener, Cacheable {

    private static final String DEFULT_TEMPLATE_NAME = "default";
    private static final Logger logger = LoggerFactory.getLogger(NotificationTaskEventListener.class);
    
	private NotificationService notificationService = (NotificationService) ServiceRegistry.get().service("EmailService");
	private TemplateManager templateService = TemplateManager.get();
	
	private boolean sendToAll = false;
	
	public NotificationTaskEventListener() {
	    
    }
	
    public NotificationTaskEventListener(boolean sendToAll) {
        this.sendToAll = sendToAll;
    
    }
    
	protected boolean accept(TaskEvent event) {
	    Task task = event.getTask();
	    if (this.notificationService != null && task.getSubject() != null && !task.getSubject().isEmpty()) {
	        return true;
	    }
	    
	    return false;
	}
	
	protected List<String> collectRecipients(Task task, UserInfo userInfo) {
	    List<String> recipients = new ArrayList<>();
	    if (sendToAll) {
            recipients.addAll(task.getPeopleAssignments().getPotentialOwners()
                    .stream()
                    .filter(oe -> oe instanceof User)
                    .map(entity -> userInfo.getEmailForEntity(entity))
                    .collect(Collectors.toList()));
            
            task.getPeopleAssignments().getPotentialOwners()
            .stream()
            .filter(oe -> oe instanceof Group)
            .forEach(group -> {
                Iterator<OrganizationalEntity> members = userInfo.getMembersForGroup((Group) group);
                while (members.hasNext()) {
                    recipients.add(userInfo.getEmailForEntity(members.next()));
                }
            });
	    } else if (task.getTaskData().getActualOwner() != null) {
	        recipients.add(userInfo.getEmailForEntity(task.getTaskData().getActualOwner()));
	    }
        
        return recipients;
	}
	
	@Override
    public void afterTaskAddedEvent(TaskEvent event) {
        
        if (!accept(event)) {
            logger.debug("Task ({}) was not eligible for sending notification, missing subject '{}'", 
                         event.getTask(), 
                         event.getTask().getSubject());
            return;
        }
        UserInfo userInfo = (UserInfo) ((TaskContext)event.getTaskContext()).get(EnvironmentName.TASK_USER_INFO);        
        Task task = event.getTask();
        
        List<String> recipients = collectRecipients(task, userInfo);
        
        if (recipients.isEmpty()) {
            logger.debug("No recipients selected, skiping sending notification for task {}", task.getId());
            return;
        }
        
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
        message.setSubject(task.getSubject());        
        message.setRecipients(recipients);
        
        String template = message.getTemplate();
        if (template == null) {
            template = DEFULT_TEMPLATE_NAME;
        }
        message.setContent(templateService.render(template, message.getData()));        
        
        notificationService.send(message);
    }
	
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

    @Override
    public void close() {
    }

}
