package org.jbpm.extensions.notifications.impl.extractors;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimeType;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.jbpm.extensions.notifications.api.MessageExtractor;

public abstract class AbstractEmailMessageExtractor implements MessageExtractor {
    
    private Pattern extractPattern = Pattern.compile("<([\\S&&[^\\}]]+)>");

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

	protected String getSourceId(Message source) throws MessagingException {
	    String sourceMessageId = null;
        if (source.getHeader("Message-Id") != null) {
            sourceMessageId = source.getHeader("Message-Id")[0];
        }
        
        return sourceMessageId;
	}
	
   protected String getReplyToId(Message source) throws MessagingException {
        String inReplyMessageId = "";
        if (source.getHeader("In-Reply-To") != null) {
            inReplyMessageId = source.getHeader("In-Reply-To")[0];
        }
        
        return inReplyMessageId;
    }
	
	   
    protected String extract(String value) {
        Matcher matcher = extractPattern.matcher(value);
        while (matcher.find()) {
            value = matcher.group(1);
        }
        
        return value;
    }
}
