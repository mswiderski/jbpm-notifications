package org.jbpm.extensions.notifications.impl.extractors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import javax.activation.MimeType;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;

import org.jbpm.document.Document;
import org.jbpm.document.service.impl.DocumentImpl;
import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.impl.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPartTextEmailMessageExtractor extends AbstractEmailMessageExtractor {

	private static final Logger logger = LoggerFactory.getLogger(MultiPartTextEmailMessageExtractor.class);
	
	private List<String> supportedContentTypes = Arrays.asList("multipart/alternative", "multipart/mixed");
	
	
	@Override
	public boolean accept(Object rawMessage) {
		if (rawMessage instanceof javax.mail.Message) {
			javax.mail.Message source = (javax.mail.Message) rawMessage;
			boolean accepted = supports(source);
			if (accepted) {
				
				try {
					Multipart multipartContent = (Multipart) source.getContent();
					
					int numberOfParts = multipartContent.getCount();
					
					for (int i = 0; i < numberOfParts; i++) {
						BodyPart part = multipartContent.getBodyPart(i);
						MimeType mimeType = new MimeType(part.getContentType());
						if (mimeType.getBaseType().equals("text/plain")) {
							return true;
						}
					}
				} catch (Exception e) {
					logger.warn("Unexpected exception while reading message body parts", e);
				}
			}
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
			
			String content = "";
			Multipart multipartContent = (Multipart) source.getContent();
			try {
				int numberOfParts = multipartContent.getCount();
				
				for (int i = 0; i < numberOfParts; i++) {
					BodyPart part = multipartContent.getBodyPart(i);
					MimeType mimeType = new MimeType(part.getContentType());
					if (mimeType.getBaseType().equals("text/plain")) {
						content = part.getContent().toString();
						break;
					}
				}
				
				// handle assignments
			} catch (Exception e) {
				throw new RuntimeException("Failed at reading message content", e);
			}
		
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
	    	
	    	List<Document> attachments = retrieveAttachments(multipartContent);
	    	for (Document doc : attachments) {
	    		data.put(doc.getName(), doc);
	    	}
	    	if (attachments.size() == 1) {
	    		data.put("attachment", attachments.get(0));
	    	} else if (!attachments.isEmpty()) {
	    		data.put("attachments", attachments);
	    	}
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

	protected List<Document> retrieveAttachments(Multipart multipartContent) {
		List<Document> attachments = new ArrayList<>();
		try {
			int numberOfParts = multipartContent.getCount();
			
			for (int i = 0; i < numberOfParts; i++) {
				BodyPart part = multipartContent.getBodyPart(i);

				if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || Part.INLINE.equalsIgnoreCase(part.getDisposition())) {

					InputStream is = part.getInputStream();

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					byte[] buf = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buf)) != -1) {
						bos.write(buf, 0, bytesRead);
					}
					bos.close();

					byte[] content = bos.toByteArray();
					DocumentImpl doc = new DocumentImpl(UUID.randomUUID().toString(), part.getFileName(),
							content.length, new Date());
					doc.setContent(content);
					attachments.add(doc);
				}
			}
		} catch (Exception e) {
			logger.warn("Error when reading attachments", e);
		}
		
		return attachments;
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
