package de.dytanic.cloudnet.ext.bridge.proxy;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.Collection;
import java.util.HashSet;

public class PlayerFallbackProfile {

    private String connectingService;
    private final Collection<String> failedConnections = new HashSet<>();

    public boolean canConnect(ServiceInfoSnapshot serviceInfoSnapshot) {
        return !this.failedConnections.contains(serviceInfoSnapshot.getName());
    }

    public Collection<String> getFailedConnections() {
        return this.failedConnections;
    }

    public String getConnectingService() {
        return this.connectingService;
    }

    public void setConnectingService(String connectingService) {
        this.connectingService = connectingService;
    }

    public void addKick(String kickedFrom) {
        this.failedConnections.add(kickedFrom);
    }

}
