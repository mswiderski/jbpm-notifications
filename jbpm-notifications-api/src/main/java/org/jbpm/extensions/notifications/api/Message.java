package org.jbpm.extensions.notifications.api;

import java.util.List;
import java.util.Map;

public interface Message {
	
	String getMessageId();
	
	String getTemplate();

	String getSender();
	
	List<String> getRecipients();
	
	String getSubject();
	
	Object getContent();
	
	String getContentType(); 
	
	Map<String, Object> getData();
	
	String getSourceMessageId(); 
}
