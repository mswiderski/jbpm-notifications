package org.jbpm.extensions.notifications.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.mail.Address;

import org.jbpm.extensions.notifications.api.Message;

public class TextEmailMessageExtractor extends AbstractEmailMessageExtractor {

	private List<String> supportedContentTypes = Arrays.asList("text/plain");	
	
	@Override
	public boolean accept(Object rawMessage) {
		if (rawMessage instanceof javax.mail.Message) {
			
			return supports((javax.mail.Message) rawMessage);
		}
		return false;
	}

	@Override
	public Message extract(Object rawMessage) {
		try {
			javax.mail.Message source = (javax.mail.Message) rawMessage;
				
			MessageImpl message = new MessageImpl();		
			message.setSubject(source.getSubject());        
			message.setMessageId(extract(getReplyToId(source)));
			message.setSourceMessageId(getSourceId(source));
			
	    	String content = source.getContent().toString();
	    	
	    	StringBuilder trimmedContent = new StringBuilder();
	    	Scanner scanner = new Scanner(content);
	    	while (scanner.hasNextLine()) {
	    		String line = scanner.nextLine();
	    		if (line.startsWith(">")) {
	    			break;
	    		}
	    		trimmedContent.append(line).append("\n");
	    	}
	    	scanner.close();
	    	
	    	Map<String, Object> data = new HashMap<>();
	    	data.put("messageContent", trimmedContent.toString());
	    	message.setData(data);
	    	
	    	message.setContent(content);
	    	Address[] senders = source.getFrom();
	    	if (senders == null || senders.length == 0) {
	    		throw new IllegalArgumentException("Message does not have sender, ignoring");
	    	}
	    	
	    	String sender = senders[0].toString();
	    	
	    	message.setSender(extract(sender));
			return message;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}



	@Override
	public List<String> getSupportedContentTypes() {
		return supportedContentTypes;
	}

	@Override
	public Integer getPriority() {
		return 10;
	}
}
