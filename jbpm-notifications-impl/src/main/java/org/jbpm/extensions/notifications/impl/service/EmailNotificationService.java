package org.jbpm.extensions.notifications.impl.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.activation.DataHandler;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.MessageExtractor;
import org.jbpm.extensions.notifications.api.ReceivedMessageCallback;
import org.jbpm.extensions.notifications.api.service.NotificationService;
import org.jbpm.extensions.notifications.api.service.RecipientService;
import org.jbpm.extensions.notifications.impl.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IdleManager;

public class EmailNotificationService implements NotificationService {

	private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
	private static final String MAIL_JNDI_KEY = System.getProperty("org.kie.mail.session", "mail/jbpmMailSession");
	
	private ExecutorService es;
	
	private List<MessageExtractor> messageExtractors = new ArrayList<>();
	
	private Store store;
	private Folder folder;
	
	private IdleManager idleManager;
	
	private List<ReceivedMessageCallback> callbacks = new CopyOnWriteArrayList<>();
	
	private RecipientService recipientService = (RecipientService) ServiceRepository.get().getService("RecipientService");
	
	private Properties emailServiceConfiguration = new Properties();
	
	public EmailNotificationService() {
		
		try {
			emailServiceConfiguration.load(this.getClass().getResourceAsStream("/email-service.properties"));
		} catch (IOException e) {
			throw new RuntimeException("Unable to load configuration of Email Service", e);
		}
	}
	
	@Override
	public void send(Message message) {
		Session session = getSession(emailServiceConfiguration.getProperty("smtp.host"), emailServiceConfiguration.getProperty("smtp.port"), emailServiceConfiguration.getProperty("username"), emailServiceConfiguration.getProperty("password"), true);
		
		javax.mail.Message msg =  null;
        try {
            msg = new MimeMessage( session ){

				@Override
				protected void updateMessageID() throws MessagingException {
					setHeader("Message-ID", "<" + message.getMessageId() + "@" + emailServiceConfiguration.getProperty("domain") + ">");
				}
            	
            };
            msg.setFrom( new InternetAddress(emailServiceConfiguration.getProperty("smtp.from")) );
            msg.setReplyTo( new InternetAddress[] {  new InternetAddress(emailServiceConfiguration.getProperty("smtp.replyto")) }  );
            
            for ( String recipient : message.getRecipients() ) {
               
                msg.addRecipients( javax.mail.Message.RecipientType.TO, InternetAddress.parse( recipientService.getAddress(recipient), false ) );
            }
            
            
            msg.setDataHandler( new DataHandler( new ByteArrayDataSource( message.getContent().toString().getBytes("UTF-8"), "text/html" ) ) );
        
            msg.setSubject(message.getSubject());            
            msg.setSentDate( new Date() );
            
            Transport t = (Transport)session.getTransport("smtp");
            try {
                t.connect(emailServiceConfiguration.getProperty("smtp.host"), Integer.valueOf(emailServiceConfiguration.getProperty("smtp.port")), emailServiceConfiguration.getProperty("username"), emailServiceConfiguration.getProperty("password"));
                t.sendMessage(msg, msg.getAllRecipients());
            } catch (Exception e) {
                throw new RuntimeException( "Connection failure", e );
            } finally {
                t.close();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to send email", e );
        }
        
        

	}

	@Override
	public void start(ReceivedMessageCallback... callback) {
		try {
			this.es = Executors.newCachedThreadPool();
			this.callbacks.addAll(Arrays.asList(callback));
			logger.debug("Email notification service starting with callbacks {}", this.callbacks);
			
			ServiceLoader<MessageExtractor> loaded = ServiceLoader.load(MessageExtractor.class);
			loaded.forEach(me -> messageExtractors.add(me));
			Collections.sort(messageExtractors, new Comparator<MessageExtractor>() {

				@Override
				public int compare(MessageExtractor o1, MessageExtractor o2) {					
					return o1.getPriority().compareTo(o2.getPriority());
				}
				
			});
			logger.info("Discovered message extractors {}", messageExtractors);
			Session session = getSession();
			
			store = session.getStore("imaps");	
			store.connect(emailServiceConfiguration.getProperty("host"), emailServiceConfiguration.getProperty("username"), emailServiceConfiguration.getProperty("password"));

			folder = store.getFolder(emailServiceConfiguration.getProperty("inbox.folder"));
			folder.open(Folder.READ_WRITE);
			folder.addMessageCountListener(new MessageCountAdapter() {
	
				@Override
				public void messagesAdded(MessageCountEvent event) {
										
					javax.mail.Message[] messages = event.getMessages();
	
	                for (javax.mail.Message message : messages) {
	                    try {
	                    	
	                    	MessageExtractor extractor = messageExtractors.stream()
	                    			.filter(me -> me.accept(message))
	                    			.findFirst()
	                    			.get();
	                    	
	                    	Message extracted = extractor.extract(message);
	                    	if (extracted == null) {
	                    		logger.info("Message extraction returned no message");
	                    		continue;
	                    	}
	                    	
	                    	logger.info("Message received and exctracted {}", extracted);
	                    	
	                    	callbacks.forEach( callback -> callback.onMessage(extracted));
	                    } catch (Exception ex) {
	                        logger.error("Unexpected error when processing received message {}", message, ex);
	                    }
	                }
	                try {
						idleManager.watch(folder);
					} catch (Exception e) {
						logger.error("Error when setting email watcher", e);
					}
					
				}
				
			});
			idleManager = new IdleManager(session, es);
			idleManager.watch(folder);
			logger.info("Email notification service started successfully at {}", new Date());
			
			ServiceRepository.get().addService("EmailService", this);
		} catch (Exception e) {
			logger.error("Email notification failed to start", e);
		}

	}

	@Override
	public void stop() {
		this.es.shutdownNow();
		if (idleManager != null) {
			idleManager.stop();
			try {
				folder.close(true);
			} catch (MessagingException e) {
				logger.error("Error when slosing inbox folder", e);
			}
			try {
				store.close();
			} catch (MessagingException e) {
				logger.error("Error when slosing IMAP store", e);
			}
		}
		
		logger.info("Email notification service stopped at {}", new Date());
	}
	
	protected Session getSession() {
		try {
			Session session = InitialContext.doLookup(MAIL_JNDI_KEY);
			logger.debug("Mail session taken from JNDI...");
			return session;
		} catch (NamingException e) {
			Properties properties = System.getProperties();
			properties.setProperty("mail.store.protocol", "imaps");
			properties.setProperty("mail.imaps.usesocketchannels", "true");

			Session session = Session.getInstance(properties, null);
			logger.debug("No mail session in JNDI, created manually...");
			return session;
		}
	}
	
	private static Session getSession(String host, String port, String username, String password, boolean startTls) {

        Session session = null;
        try {
        	session = InitialContext.doLookup(MAIL_JNDI_KEY);
        } catch (NamingException e1) {
            
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.port", port);
	        
	        if(startTls) { 
	            properties.put("mail.smtp.starttls.enable","true");
	        }     
	        if( username != null ) { 
	            properties.setProperty("mail.smtp.submitter", username);
	            if( password != null) {
	                Authenticator authenticator = new Authenticator(username, password);
	                properties.setProperty("mail.smtp.auth", "true");
	                session = Session.getInstance(properties, authenticator);
	            }
	            else { 
	                session = Session.getInstance(properties);
	            }
	        }
	        else { 
	            session = Session.getInstance(properties);
	        }

        }
       
        return session;
    }
	
	private static class Authenticator extends javax.mail.Authenticator {
        private PasswordAuthentication authentication;

        public Authenticator(String username, String password) {
            authentication = new PasswordAuthentication(username, password);
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return authentication;
        }
    }

}
