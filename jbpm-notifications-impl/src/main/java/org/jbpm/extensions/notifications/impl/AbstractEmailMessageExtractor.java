package org.jbpm.extensions.notifications.impl;

import java.util.List;

import javax.activation.MimeType;
import javax.mail.Message;

import org.jbpm.extensions.notifications.api.MessageExtractor;

public abstract class AbstractEmailMessageExtractor implements MessageExtractor {


	public abstract List<String> getSupportedContentTypes();
	
	protected boolean supports(Message emailMessage) {
		
		List<String> supportedContentTypes = getSupportedContentTypes();
		try {
			String contentType = emailMessage.getContentType();
			MimeType mimeType = new MimeType(contentType);
			return supportedContentTypes.contains(mimeType.getBaseType());
		} catch (Exception e) {
			throw new RuntimeException("Unable to determine supported content types", e);
		}
	}

}
