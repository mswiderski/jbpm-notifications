package org.jbpm.extensions.notifications.kieserver;

import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServer;
import org.kie.server.services.api.KieServerEventListener;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.impl.KieServerImpl;


public class NotificationKieServerEventListener implements KieServerEventListener {

    @Override
    public void beforeServerStarted(KieServer kieServer) {   
    }

    @Override
    public void afterServerStarted(KieServer kieServer) {
        
        KieServerExtension notificationExtension = ((KieServerImpl)kieServer).getServerRegistry().getServerExtension(NotificationKieServerExtension.EXTENSION_NAME);
        
        ((NotificationKieServerExtension) notificationExtension).startNotificationService();
    }

    @Override
    public void beforeServerStopped(KieServer kieServer) {
    }

    @Override
    public void afterServerStopped(KieServer kieServer) {
    }

    @Override
    public void beforeContainerStarted(KieServer kieServer, KieContainerInstance containerInstance) {
    }

    @Override
    public void afterContainerStarted(KieServer kieServer, KieContainerInstance containerInstance) {
    }

    @Override
    public void beforeContainerStopped(KieServer kieServer, KieContainerInstance containerInstance) {
    }

    @Override
    public void afterContainerStopped(KieServer kieServer, KieContainerInstance containerInstance) {
    }

}
