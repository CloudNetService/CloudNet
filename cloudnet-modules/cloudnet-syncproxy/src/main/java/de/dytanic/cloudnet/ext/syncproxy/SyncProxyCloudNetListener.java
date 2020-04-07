package de.dytanic.cloudnet.ext.syncproxy;


import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;

public class SyncProxyCloudNetListener {

    private final AbstractSyncProxyManagement syncProxyManagement;

    public SyncProxyCloudNetListener(AbstractSyncProxyManagement syncProxyManagement) {
        this.syncProxyManagement = syncProxyManagement;
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy()) {
            return;
        }

        this.syncProxyManagement.updateTabList();
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.syncProxyManagement.broadcastServiceStateChange("service-stop", event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.syncProxyManagement.broadcastServiceStateChange("service-start", event.getServiceInfo());
    }


}
