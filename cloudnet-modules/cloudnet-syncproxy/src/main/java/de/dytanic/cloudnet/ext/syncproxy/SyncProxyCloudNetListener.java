package de.dytanic.cloudnet.ext.syncproxy;


import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;

public class SyncProxyCloudNetListener {

    private final AbstractSyncProxyManagement syncProxyManagement;

    public SyncProxyCloudNetListener(AbstractSyncProxyManagement syncProxyManagement) {
        this.syncProxyManagement = syncProxyManagement;
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.syncProxyManagement.broadcastServiceStateChange("service-start", event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

        if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftProxy() && this.syncProxyManagement.inGroup(serviceInfoSnapshot)) {
            this.syncProxyManagement.removeServiceOnlineCount(serviceInfoSnapshot);
        }

        this.syncProxyManagement.broadcastServiceStateChange("service-stop", serviceInfoSnapshot);
    }

    @EventListener
    public void handleUpdate(CloudServiceInfoUpdateEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

        if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftProxy() && this.syncProxyManagement.inGroup(serviceInfoSnapshot)) {
            this.syncProxyManagement.updateServiceOnlineCount(serviceInfoSnapshot);
        }
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME) || event.getMessage() == null) {
            return;
        }

        if (SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION.equals(event.getMessage().toLowerCase())) {
            SyncProxyConfiguration syncProxyConfiguration = event.getData().get("syncProxyConfiguration", SyncProxyConfiguration.TYPE);

            this.syncProxyManagement.setSyncProxyConfiguration(syncProxyConfiguration);
        }
    }

}
