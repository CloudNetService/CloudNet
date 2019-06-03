package de.dytanic.cloudnet.ext.cloudperms.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.cloudperms.node.CloudNetCloudPermissionsModule;

public final class ConfigurationUpdateListener {

    @EventListener
    public void handle(NetworkChannelAuthClusterNodeSuccessEvent event)
    {
        event.getNode().sendCustomChannelMessage(
            "cloudnet_cloudperms_update",
            "update_configuration",
            CloudNetCloudPermissionsModule.getInstance().getConfig()
        );
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event)
    {
        if (event.getChannel().equalsIgnoreCase("cloudnet_cloudperms_update") &&
            event.getMessage() != null && event.getMessage().equalsIgnoreCase("update_configuration"))
        {
            CloudNetCloudPermissionsModule.getInstance().getConfig().append("enabled", event.getData().getBoolean("enabled"));
            CloudNetCloudPermissionsModule.getInstance().getConfig().append("excludedGroups", event.getData().get("excludedGroups"));
            CloudNetCloudPermissionsModule.getInstance().saveConfig();
        }
    }
}