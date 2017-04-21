package org.jbpm.extensions.notifications.api;

public interface MessageExtractor {

	boolean accept(Object rawMessage);
	
	Message extract(Object rawMessage);
	
	Integer getPriority();
}
