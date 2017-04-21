package org.jbpm.extensions.notifications.impl;

import java.util.List;
import java.util.Map;

import org.jbpm.extensions.notifications.api.Message;

public class MessageImpl implements Message {

	private String messageId;
	
	private String template;
	
	private String sender;
	
	private List<String> recipients;
	
	private String subject;
	
	private Object content;
	
	private Map<String, Object> data;

	@Override
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	@Override
	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<String> recipients) {
		this.recipients = recipients;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}

	@Override
	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	@Override
	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	@Override
	public String toString() {
		return "MessageImpl [messageId=" + messageId + ", sender=" + sender + ", recipients=" + recipients
				+ ", subject=" + subject + ", content=" + content + ", data=" + data + "]";
	}
	
}
