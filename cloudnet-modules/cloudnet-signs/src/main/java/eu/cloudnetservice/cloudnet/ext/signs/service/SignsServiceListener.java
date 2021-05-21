package eu.cloudnetservice.cloudnet.ext.signs.service;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;

public class SignsServiceListener {

    protected final ServiceSignManagement<?> signManagement;

    public SignsServiceListener(ServiceSignManagement<?> signManagement) {
        this.signManagement = signManagement;
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        this.signManagement.handleServiceAdd(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.signManagement.handleServiceUpdate(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        this.signManagement.handleServiceUpdate(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        this.signManagement.handleServiceUpdate(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        this.signManagement.handleServiceUpdate(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.signManagement.handleServiceUpdate(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        this.signManagement.handleServiceRemove(event.getServiceInfo());
    }
}
