package org.jbpm.extensions.notifications.impl.service;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.mail.Flags;
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
import javax.mail.search.FlagTerm;
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

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IdleManager;
import com.sun.mail.imap.protocol.IMAPProtocol;

public class EmailNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private ScheduledExecutorService keepAlive = Executors.newScheduledThreadPool(1);
    private static final int KEEP_ALIVE_FREQ = Integer.parseInt(System.getProperty("org.jbpm.notifications.keepalive.interval", "5"));

    private ExecutorService es;
    private boolean executorServiceManaged = false;

    private List<MessageExtractor> messageExtractors = new ArrayList<>();

    private Store store;
    private Folder folder;

    private IdleManager idleManager;

    private List<ReceivedMessageCallback> callbacks = new CopyOnWriteArrayList<>();

    private Properties emailServiceConfiguration;
    private RecipientService recipientService;

    public EmailNotificationService(RecipientService recipientService, Properties emailServiceConfiguration) {
        this.recipientService = recipientService;
        this.emailServiceConfiguration = emailServiceConfiguration;

    }

    @Override
    public void send(Message message) {
        Session session = getSession(emailServiceConfiguration.getProperty("smtp.host"), emailServiceConfiguration.getProperty("smtp.port"), emailServiceConfiguration.getProperty("username"), emailServiceConfiguration
                                                                                                                                                                                                                         .getProperty("password"),
                                     true);
        String subjectPrefix = "";
        javax.mail.Message msg = null;
        try {
            msg = new MimeMessage(session) {

                @Override
                protected void updateMessageID() throws MessagingException {
                    setHeader("Message-ID", "<" + message.getMessageId() + "@" + emailServiceConfiguration.getProperty("domain") + ">");
                }

            };
            msg.setFrom(new InternetAddress(emailServiceConfiguration.getProperty("smtp.from")));
            msg.setReplyTo(new InternetAddress[]{new InternetAddress(emailServiceConfiguration.getProperty("smtp.replyto"))});

            for (String recipient : message.getRecipients()) {

                msg.addRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(recipient, false));
            }
            if (message.getSourceMessageId() != null) {
                msg.setHeader("In-Reply-To", message.getSourceMessageId());
                if (!message.getSubject().startsWith("Re:")) {
                    subjectPrefix = "Re:";
                }
            }

            msg.setDataHandler(new DataHandler(new ByteArrayDataSource(message.getContent().toString().getBytes("UTF-8"), message.getContentType())));

            msg.setSubject(subjectPrefix + message.getSubject());
            msg.setSentDate(new Date());

            Transport t = (Transport) session.getTransport("smtp");
            try {
                t.connect(emailServiceConfiguration.getProperty("smtp.host"), Integer.valueOf(emailServiceConfiguration.getProperty("smtp.port")), emailServiceConfiguration.getProperty("username"),
                          emailServiceConfiguration.getProperty("password"));
                t.sendMessage(msg, msg.getAllRecipients());
            } catch (Exception e) {
                throw new RuntimeException("Connection failure", e);
            } finally {
                t.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to send email", e);
        }

    }

    @Override
    public void start(ReceivedMessageCallback... callback) {
        try {
            try {
                this.es = InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
                this.executorServiceManaged = true;
            } catch (Exception e) {
                this.es = Executors.newCachedThreadPool();
            }
            parseAdditionalCallbacks();
            this.callbacks.addAll(Arrays.asList(callback));
            logger.debug("Email notification service starting with callbacks {}", this.callbacks);

            ServiceLoader<MessageExtractor> loaded = ServiceLoader.load(MessageExtractor.class);
            loaded.forEach(me -> messageExtractors.add(me));

            parseAdditionalMessageExtractors();

            Collections.sort(messageExtractors, new Comparator<MessageExtractor>() {

                @Override
                public int compare(MessageExtractor o1, MessageExtractor o2) {
                    return o1.getPriority().compareTo(o2.getPriority());
                }

            });
            logger.info("Discovered message extractors {}", messageExtractors);
            Session session = getSession();

            store = session.getStore("imaps");
            store.connect(emailServiceConfiguration.getProperty("host"),
                          Integer.parseInt(emailServiceConfiguration.getProperty("port")),
                          emailServiceConfiguration.getProperty("username"),
                          emailServiceConfiguration.getProperty("password"));

            folder = store.getFolder(emailServiceConfiguration.getProperty("inbox.folder"));
            folder.open(Folder.READ_WRITE);
            folder.addMessageCountListener(new MessageCountAdapter() {

                @Override
                public void messagesRemoved(MessageCountEvent event) {
                    try {
                        idleManager.watch(folder);
                    } catch (Exception e) {
                        logger.error("Error when setting email watcher", e);
                    }
                }

                @Override
                public void messagesAdded(MessageCountEvent event) {

                    javax.mail.Message[] messages = event.getMessages();

                    for (javax.mail.Message message : messages) {
                        processMessage(message);
                    }
                    try {
                        idleManager.watch(folder);
                    } catch (Exception e) {
                        logger.error("Error when setting email watcher", e);
                    }

                }

            });            

            ServiceRepository.get().addService("EmailService", this);
            
            Flags seen = new Flags(Flags.Flag.SEEN);
            FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
            javax.mail.Message[] unreadMessages = folder.search(unseenFlagTerm);
            logger.info("Search for unread messages with result {}", unreadMessages.length);
            
            for (javax.mail.Message msg : unreadMessages) {
                processMessage(msg);
            }
            
            idleManager = new IdleManager(session, es);
            idleManager.watch(folder);
            keepAlive.scheduleAtFixedRate(() -> keepAliveRunner(),
                                          KEEP_ALIVE_FREQ,
                                          KEEP_ALIVE_FREQ,
                                          TimeUnit.MINUTES);
            logger.info("Email notification service started successfully at {}", new Date());
            
        } catch (Exception e) {
            logger.error("Email notification failed to start", e);
        }

    }

    @Override
    public void stop() {
        keepAlive.shutdownNow();

        if (!this.executorServiceManaged) {
            this.es.shutdownNow();
        }
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
            Session session = InitialContext.doLookup(emailServiceConfiguration.getProperty("mailSession"));
            Properties props = session.getProperties();
            props.put("mail.event.scope", "session");
            props.put("mail.event.executor", es);
            logger.debug("Mail session taken from JNDI...");
            return session;
        } catch (NamingException e) {
            Properties properties = System.getProperties();
            properties.setProperty("mail.store.protocol", "imaps");
            properties.setProperty("mail.imaps.usesocketchannels", "true");
            properties.setProperty("mail.event.scope", "session");
            properties.put("mail.event.executor", es);

            Session session = Session.getInstance(properties, null);
            logger.debug("No mail session in JNDI, created manually...");
            return session;
        }
    }

    protected Session getSession(String host, String port, String username, String password, boolean startTls) {

        Session session = null;
        try {
            session = InitialContext.doLookup(emailServiceConfiguration.getProperty("mailSession"));
        } catch (NamingException e1) {

            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.port", port);

            if (startTls) {
                properties.put("mail.smtp.starttls.enable", "true");
            }
            if (username != null) {
                properties.setProperty("mail.smtp.submitter", username);
                if (password != null) {
                    Authenticator authenticator = new Authenticator(username, password);
                    properties.setProperty("mail.smtp.auth", "true");
                    session = Session.getInstance(properties, authenticator);
                } else {
                    session = Session.getInstance(properties);
                }
            } else {
                session = Session.getInstance(properties);
            }

        }

        return session;
    }

    protected void parseAdditionalCallbacks() {

        String additionalCallbacks = emailServiceConfiguration.getProperty("callbacks");
        if (additionalCallbacks != null && !additionalCallbacks.isEmpty()) {
            String[] callbackClasses = additionalCallbacks.split(",");

            for (String callbackClass : callbackClasses) {
                try {
                    Class<?> clazz = Class.forName(callbackClass, true, this.getClass().getClassLoader());

                    ReceivedMessageCallback instance = (ReceivedMessageCallback) clazz.newInstance();
                    callbacks.add(instance);
                } catch (Exception e) {
                    logger.warn("Unable to create instance of callback for class name {}", callbackClass, e);
                }
            }
        }
    }

    protected void parseAdditionalMessageExtractors() {

        String additionalExtractors = emailServiceConfiguration.getProperty("extractors");
        if (additionalExtractors != null && !additionalExtractors.isEmpty()) {
            String[] extractorsClasses = additionalExtractors.split(",");

            for (String extractorsClass : extractorsClasses) {
                try {
                    Class<?> clazz = Class.forName(extractorsClass, true, this.getClass().getClassLoader());

                    MessageExtractor instance = (MessageExtractor) clazz.newInstance();
                    messageExtractors.add(instance);
                } catch (Exception e) {
                    logger.warn("Unable to create instance of message extractor for class name {}", extractorsClass, e);
                }
            }
        }
    }
    
    protected void processMessage(javax.mail.Message message) {
        try {

            MessageExtractor extractor = messageExtractors.stream()
                                                          .filter(me -> me.accept(message))
                                                          .findFirst()
                                                          .get();

            Message extracted = extractor.extract(message);
            if (extracted == null) {
                logger.info("Message extraction returned no message");
                return;
            }

            logger.info("Message received and exctracted {}", extracted);

            for (ReceivedMessageCallback callback : callbacks) {
                try {
                    callback.onMessage(recipientService, extracted);
                } catch (Exception e) {
                    logger.warn("Error when invoking callback {} with error {}", callback, e.getMessage(), e);
                }
            }
        } catch (Exception ex) {
            logger.error("Unexpected error when processing received message {}", message, ex);
        }
    }

    public void keepAliveRunner() {
        try {
            logger.debug("Sending NOOP command to keep it alive");
            ((IMAPFolder) folder).doCommand(new IMAPFolder.ProtocolCommand() {

                public Object doCommand(IMAPProtocol p) throws ProtocolException {
                    p.simpleCommand("NOOP", null);
                    return null;
                }
            });
        } catch (MessagingException e) {
            logger.warn("Unable to send NOOP command", e);
        }
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
