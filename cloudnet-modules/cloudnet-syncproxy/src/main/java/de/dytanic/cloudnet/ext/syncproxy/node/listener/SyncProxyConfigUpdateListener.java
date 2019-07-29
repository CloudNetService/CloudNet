package de.dytanic.cloudnet.ext.syncproxy.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

public final class SyncProxyConfigUpdateListener {

    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase(SyncProxyConstants.SYNC_PROXY_SYNC_CHANNEL_PROPERTY)) {
            return;
        }

        switch (event.getId()) {
            case SyncProxyConstants.SIGN_CHANNEL_SYNC_ID_GET_SYNC_PROXY_CONFIGURATION_PROPERTY: {
                event.setCallbackPacket(new JsonDocument("syncProxyConfiguration", CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration()));
            }
            break;
        }
    }

    @EventListener
    public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
        CloudNetDriver.getInstance().sendChannelMessage(
                SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                new JsonDocument("syncProxyConfiguration", CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
        );
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equalsIgnoreCase(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION: {
                SyncProxyConfiguration syncProxyConfiguration = event.getData().get("syncProxyConfiguration", SyncProxyConfiguration.TYPE);

                if (syncProxyConfiguration != null) {
                    CloudNetSyncProxyModule.getInstance().setSyncProxyConfiguration(syncProxyConfiguration);
                }

                SyncProxyConfigurationWriterAndReader.write(syncProxyConfiguration, CloudNetSyncProxyModule.getInstance().getConfigurationFile());
            }
            break;
        }
    }
}