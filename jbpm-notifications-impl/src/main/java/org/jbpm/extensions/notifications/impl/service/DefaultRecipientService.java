package org.jbpm.extensions.notifications.impl.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jbpm.extensions.notifications.api.service.RecipientService;
import org.jbpm.extensions.notifications.impl.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRecipientService implements RecipientService {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultRecipientService.class);

	private Map<String, String> mappedUserToAddress = new HashMap<>();
	private Map<String, String> mappedAddressToUser = new HashMap<>();
	
	public DefaultRecipientService() {
		ServiceRepository.get().addService("RecipientService", this);
		try {
    		Properties recipients = new Properties();
    		recipients.load(this.getClass().getResourceAsStream("/recipient-service.properties"));
    		
    		recipients.forEach((Object k, Object v) -> add(k.toString(), v.toString()));
		} catch (Exception e) {
		    logger.warn("Failed to load default set of recipients", e);
		}
	}
	
	@Override
	public String getAddress(String userId) {
		
		return mappedUserToAddress.get(userId);
	}

	@Override
	public String getUserId(String address) {
		return mappedAddressToUser.get(address);
	}
	
	public void add(String user, String email) {
		this.mappedAddressToUser.put(email, user);
		this.mappedUserToAddress.put(user, email);
	}

}
