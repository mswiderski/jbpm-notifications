package org.jbpm.extensions.notifications.kieserver;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.jbpm.extensions.notifications.api.ReceivedMessageCallback;
import org.jbpm.extensions.notifications.api.service.MessageTemplateService;
import org.jbpm.extensions.notifications.api.service.NotificationService;
import org.jbpm.extensions.notifications.api.service.RecipientService;
import org.jbpm.extensions.notifications.impl.callbacks.CompleteUserTaskCallback;
import org.jbpm.extensions.notifications.impl.service.DefaultRecipientService;
import org.jbpm.extensions.notifications.impl.service.EmailNotificationService;
import org.jbpm.extensions.notifications.impl.service.FreeMarkerMessageTemplateService;
import org.jbpm.extensions.notifications.impl.utils.Helper;
import org.jbpm.services.api.UserTaskService;
import org.kie.scanner.KieModuleMetaData;
import org.kie.server.api.KieServerConstants;
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

    private UserTaskService userTaskService;
    private boolean initialized = false;

    private KieServerRegistry registry;

	private ReceivedMessageCallback callback;
	private NotificationService notificationService;
	private MessageTemplateService templateService;

	private Map<String, Set<String>> knownTemplates = new ConcurrentHashMap<>();

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
		this.registry = registry;
        KieServerExtension jbpmExtension = registry.getServerExtension("jBPM");
        if (jbpmExtension == null) {
            initialized = false;
            logger.warn("jBPM extension not found, jBPM Notifications cannot work without jBPM extension, disabling itself");
            return;
        }

        List<Object> jbpmServices = jbpmExtension.getServices();

        for( Object object : jbpmServices ) {
            // in case given service is null (meaning was not configured) continue with next one
            if (object == null) {
                continue;
            }

            if( UserTaskService.class.isAssignableFrom(object.getClass()) ) {
                userTaskService = (UserTaskService) object;
                continue;
            }
        }
		RecipientService recipientService = new DefaultRecipientService();
		templateService = new FreeMarkerMessageTemplateService();
		// TODO replace with proper support
		((DefaultRecipientService)recipientService).add("user", "email");
        this.callback = new CompleteUserTaskCallback(userTaskService, recipientService);
        this.notificationService = new EmailNotificationService();
        this.notificationService.start(callback);
        this.initialized = true;
	}

	@Override
	public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
		this.notificationService.stop();

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

	}

	@Override
	public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {

		knownTemplates.get(id).forEach(templateId -> {
			templateService.unregisterTemplate(templateId);
		});
	}

	@Override
	public boolean isUpdateContainerAllowed(String id, KieContainerInstance kieContainerInstance,
			Map<String, Object> parameters) {
		return true;
	}

	@Override
	public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {

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

}
