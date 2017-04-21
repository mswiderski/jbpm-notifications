package org.jbpm.extensions.notifications.impl.service;

import java.io.FileNotFoundException;
import java.io.StringWriter;

import org.jbpm.extensions.notifications.api.Message;
import org.jbpm.extensions.notifications.api.service.MessageTemplateService;
import org.jbpm.extensions.notifications.impl.ServiceRepository;
import org.jbpm.extensions.notifications.impl.utils.Helper;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class FreeMarkerMessageTemplateService implements MessageTemplateService {

	private static final String DEFULT_TEMPLATE_NAME = "default";
	private static final String RESOURCE_TYPE = "-email\\.ftl";
	
	private StringTemplateLoader stringLoader = new StringTemplateLoader();
	private Configuration cfg;
	
	public FreeMarkerMessageTemplateService() {
		cfg = new Configuration();
        cfg.setTemplateLoader(stringLoader);
        cfg.setDefaultEncoding("UTF-8");
        
        ServiceRepository.get().addService("TemplateService", this);
        
        String defaultTemplate = Helper.read(this.getClass().getResourceAsStream("/default-email.ftl"));
        registerTemplate(DEFULT_TEMPLATE_NAME, defaultTemplate);
	}
	
	@Override
	public String apply(Message message) {
		String templateName = message.getTemplate();
		if (templateName == null) {
			templateName = DEFULT_TEMPLATE_NAME;
		}
		
		StringWriter out = new StringWriter();
		try {
			Template template = null;
			
			try {
				template = cfg.getTemplate(templateName);
			} catch (FileNotFoundException fe) {
				template = cfg.getTemplate(DEFULT_TEMPLATE_NAME);
			}
			
			template.process(message.getData(), out);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return out.toString();
	}

	@Override
	public void registerTemplate(String id, Object template) {
		this.stringLoader.putTemplate(id, template.toString());

	}

	@Override
	public void unregisterTemplate(String id) {
		// no-op

	}

	@Override
	public String getResourceType() {
		return RESOURCE_TYPE;
	}

}
