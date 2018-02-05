package org.jbpm.extensions.notifications.kieserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.jbpm.extensions.notifications.api.ReceivedMessageCallback;
import org.jbpm.extensions.notifications.api.service.MessageTemplateService;
import org.jbpm.extensions.notifications.api.service.NotificationService;
import org.jbpm.extensions.notifications.api.service.RecipientService;
import org.jbpm.extensions.notifications.impl.service.DefaultRecipientService;
import org.jbpm.extensions.notifications.impl.service.EmailNotificationService;
import org.jbpm.extensions.notifications.impl.service.FreeMarkerMessageTemplateService;
import org.jbpm.extensions.notifications.impl.utils.Helper;
import org.kie.scanner.KieModuleMetaData;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.KieServerEnvironment;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.KieServerImpl;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationKieServerExtension implements KieServerExtension {

	public static final String EXTENSION_NAME = "jBPM-Notifications";

    private static final Logger logger = LoggerFactory.getLogger(NotificationKieServerExtension.class);

    private static final Boolean jbpmDisabled = Boolean.parseBoolean(System.getProperty(KieServerConstants.KIE_JBPM_SERVER_EXT_DISABLED, "false"));

    private boolean initialized = false;
	
	private NotificationService notificationService;
	private MessageTemplateService templateService;
	private RecipientService recipientService;

	private Map<String, Set<String>> knownTemplates = new ConcurrentHashMap<>();
	private Map<String, NotificationService> notificationServicePerContainer = new ConcurrentHashMap<>();

	public NotificationKieServerExtension() {
		if (System.getProperty("org.kie.deployment.desc.location") == null) {
			System.setProperty("org.kie.deployment.desc.location", "classpath:/dd.xml");
		}
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public boolean isActive() {
		return jbpmDisabled == false;
	}

    @Override
    public void init(KieServerImpl kieServer, KieServerRegistry registry) {        
        KieServerExtension jbpmExtension = registry.getServerExtension("jBPM");
        if (jbpmExtension == null) {
            initialized = false;
            logger.warn("jBPM extension not found, jBPM Notifications cannot work without jBPM extension, disabling itself");
            return;
        }
        recipientService = new DefaultRecipientService();
        
        templateService = new FreeMarkerMessageTemplateService();

        try {
            Properties emailServiceConfiguration = new Properties();
            emailServiceConfiguration.load(this.getClass().getResourceAsStream("/email-service.properties"));
            
            this.notificationService = new EmailNotificationService(recipientService, emailServiceConfiguration);
            
            
        } catch (IOException e) {
            logger.info("No global notification service configuration present, email watcher not started");
        }

        this.initialized = true;
    }

	@Override
	public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
		if (this.notificationService != null) {
    	    this.notificationService.stop();
    		logger.info("Email watcher stopped for server {}", KieServerEnvironment.getServerId());
		}
	}

	@Override
	public void createContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {

		KieModuleMetaData metaData = (KieModuleMetaData) parameters.get(KieServerConstants.KIE_SERVER_PARAM_MODULE_METADATA);

		ClassLoader classloader = metaData.getClassLoader().getParent();
		if (classloader instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) classloader).getURLs();
			if (urls == null || urls.length == 0) {
				return;
			}
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.addUrls(urls);
			builder.addClassLoader(classloader);
			builder.setScanners(new ResourcesScanner());

			Reflections reflections = new Reflections(builder);

			Set<String> foundResources = reflections.getResources(Pattern.compile(".*" + templateService.getResourceType()));
			logger.debug("Found following templates {}", foundResources);

			Set<String> registeredTemplates = new HashSet<>();
			knownTemplates.put(id, registeredTemplates);

			foundResources.forEach(filePath -> {
				InputStream in = classloader.getResourceAsStream(filePath);
				if (in != null) {
					String templateId = Paths.get(filePath).getFileName().toString();
					String templateContent = Helper.read(in);

					templateService.registerTemplate(templateId.replaceFirst(templateService.getResourceType(), ""), templateContent);
					registeredTemplates.add(templateId);
				} else {
					logger.warn("Cannot load template from path {}", filePath);
				}
			});
		}
		
		
		try {
            Properties emailServiceConfiguration = new Properties();
            emailServiceConfiguration.load(kieContainerInstance.getKieContainer().getClassLoader().getResourceAsStream("/kjar-email-service.properties"));
            
            EmailNotificationService kjarNotificationService = new EmailNotificationService(recipientService, emailServiceConfiguration);
            List<ReceivedMessageCallback> callbacks = new ArrayList<>();
            collectCallbacks(kieContainerInstance.getKieContainer().getClassLoader(), callbacks);
            kjarNotificationService.start(callbacks.toArray(new ReceivedMessageCallback[callbacks.size()]));
            
            notificationServicePerContainer.put(id, kjarNotificationService);
            logger.info("Email watcher started for container {}", id);
        } catch (Exception e) {
            logger.info("No notification service configuration present in container {}, email watcher not started for container {}", id, id);
        }
	}

	@Override
	public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {

		disposeContainer(id, kieContainerInstance, parameters);
		createContainer(id, kieContainerInstance, parameters);
	}

	@Override
	public boolean isUpdateContainerAllowed(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
		return true;
	}

	@Override
	public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
	    NotificationService kjarNotificationService = notificationServicePerContainer.remove(id);
	    if (kjarNotificationService != null) {
	        kjarNotificationService.stop();
	        logger.info("Email watcher stopped for container {}", id);
	    }
	    
	    knownTemplates.get(id).forEach(templateId -> {
            templateService.unregisterTemplate(templateId);
        });
	}

	@Override
	public List<Object> getAppComponents(SupportedTransports type) {
		return new ArrayList<>();
	}

	@Override
	public <T> T getAppComponents(Class<T> serviceType) {
		return null;
	}

	@Override
	public String getImplementedCapability() {
		return "Notification";
	}

	@Override
	public List<Object> getServices() {
		return new ArrayList<>();
	}

	@Override
	public String getExtensionName() {
		return EXTENSION_NAME;
	}

	@Override
	public Integer getStartOrder() {
		return 100;
	}

	@Override
	public String toString() {
		return EXTENSION_NAME;
	}

    public void startNotificationService() {
        if (notificationService != null) {
            List<ReceivedMessageCallback> callbacks = new ArrayList<>();
            collectCallbacks(this.getClass().getClassLoader(), callbacks);
            this.notificationService.start(callbacks.toArray(new ReceivedMessageCallback[callbacks.size()]));
            logger.info("Email watcher started for server {}", KieServerEnvironment.getServerId());
        }
        
    }
    
    protected void collectCallbacks(ClassLoader cl, List<ReceivedMessageCallback> callbacks) {
        ServiceLoader<ReceivedMessageCallback> loaded = ServiceLoader.load(ReceivedMessageCallback.class, cl);
        loaded.forEach(me -> callbacks.add(me));
    }

}
