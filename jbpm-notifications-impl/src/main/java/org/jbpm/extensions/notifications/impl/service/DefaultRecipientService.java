package org.jbpm.extensions.notifications.impl.service;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.extensions.notifications.api.service.RecipientService;
import org.jbpm.extensions.notifications.impl.ServiceRepository;

public class DefaultRecipientService implements RecipientService {

	private Map<String, String> mappedUserToAddress = new HashMap<>();
	private Map<String, String> mappedAddressToUser = new HashMap<>();
	
	public DefaultRecipientService() {
		ServiceRepository.get().addService("RecipientService", this);
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
